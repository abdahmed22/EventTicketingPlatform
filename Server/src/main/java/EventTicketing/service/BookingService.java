package EventTicketing.service;

import EventTicketing.dto.BookingDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.exception.SeatUnavailableException;
import EventTicketing.model.Booking;
import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.User;
import EventTicketing.model.enums.BookingStatus;
import EventTicketing.repository.BookingRepository;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.SeatCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import EventTicketing.exception.SeatUnavailableException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final EventRepository eventRepository;
  private final SeatCategoryRepository seatCategoryRepository;

  @Transactional
  public void updateAvailableSeats(
      SeatCategory seatCategory,
      int amount) {

    int newAvailableSeats =
        seatCategory.getAvailableSeats() + amount;

    if (newAvailableSeats < 0) {
      throw new IllegalStateException(
          "Available seats cannot be negative");
    }

    if (newAvailableSeats > seatCategory.getTotalSeats()) {
      throw new IllegalStateException(
          "Available seats cannot exceed total seats");
    }

    seatCategory.setAvailableSeats(newAvailableSeats);

    seatCategoryRepository.save(seatCategory);
  }

  @Transactional
  public BookingDto.Response reserve(
      User user,
      BookingDto.CreateRequest request) {

    Event event = eventRepository
        .findById(request.eventId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Event not found"));

    // Only PUBLISHED events can be booked
    if (event.getStatus() != Event.Status.PUBLISHED) {
      throw new IllegalStateException(
          "Event is not open for booking");
    }

    SeatCategory seatCategory = seatCategoryRepository
        .findByIdWithLock(request.seatCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Seat category not found"));

    // Make sure the seat category belongs to this event
    if (!seatCategory.getEvent().getId()
        .equals(event.getId())) {

      throw new IllegalStateException(
          "Seat category does not belong to this event");
    }

    // Seat availability conflict -> 409 CONFLICT
    if (request.quantity() > seatCategory.getAvailableSeats()) {

      throw new SeatUnavailableException(
          "Not enough seats available");
    }

    BigDecimal totalPrice = seatCategory.getPrice()
        .multiply(
            BigDecimal.valueOf(
                request.quantity()));

    // Reserve seats
    updateAvailableSeats(
        seatCategory,
        -request.quantity());

    Instant now = Instant.now();

    Booking booking = Booking.builder()
        .user(user)
        .event(event)
        .seatCategory(seatCategory)
        .quantity(request.quantity())
        .totalPrice(totalPrice)
        .status(BookingStatus.PENDING)
        .createdAt(now)
        .expiresAt(
            now.plus(
                5,
                ChronoUnit.MINUTES))
        .build();

    Booking savedBooking =
        bookingRepository.save(booking);

    return BookingDto.Response.from(savedBooking);
  }

  @Transactional
  public BookingDto.Response confirm(
      UUID bookingId,
      User user) {

    Booking booking = bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Booking not found"));

    if (!booking.getUser().getId()
        .equals(user.getId())) {

      throw new ForbiddenActionException(
          "You cannot confirm this booking");
    }

    if (booking.getStatus() != BookingStatus.PENDING) {

      throw new IllegalStateException(
          "Booking cannot be confirmed");
    }

    // Check expiration
    if (Instant.now().isAfter(
        booking.getExpiresAt())) {

      expireBooking(booking);

      throw new IllegalStateException(
          "Booking reservation expired");
    }

    booking.setStatus(
        BookingStatus.CONFIRMED);

    booking.setConfirmedAt(
        Instant.now());

    Booking savedBooking =
        bookingRepository.save(booking);

    return BookingDto.Response.from(
        savedBooking);
  }

  @Transactional
  public BookingDto.Response cancel(
      UUID bookingId,
      User user) {

    Booking booking = bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Booking not found"));

    if (!booking.getUser().getId()
        .equals(user.getId())) {

      throw new ForbiddenActionException(
          "You cannot cancel this booking");
    }

    if (booking.getStatus() == BookingStatus.CANCELLED
        || booking.getStatus() == BookingStatus.EXPIRED) {

      throw new IllegalStateException(
          "Booking is already inactive");
    }

    // Lock the seat category
    SeatCategory seatCategory =
        seatCategoryRepository
            .findByIdWithLock(
                booking
                    .getSeatCategory()
                    .getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Seat category not found"));

    // Return seats
    updateAvailableSeats(
        seatCategory,
        booking.getQuantity());

    booking.setStatus(
        BookingStatus.CANCELLED);

    booking.setCancelledAt(
        Instant.now());

    Booking savedBooking =
        bookingRepository.save(booking);

    return BookingDto.Response.from(
        savedBooking);
  }

  @Transactional
  public void expireBooking(Booking booking) {

    if (booking.getStatus() != BookingStatus.PENDING) {
      return;
    }

    SeatCategory seatCategory =
        seatCategoryRepository
            .findByIdWithLock(
                booking.getSeatCategory().getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Seat category not found"));

    updateAvailableSeats(
        seatCategory,
        booking.getQuantity());

    booking.setStatus(
        BookingStatus.EXPIRED);

    bookingRepository.save(booking);
  }

  @Transactional
  public void releaseSeats(
      UUID seatCategoryId,
      int quantity) {

    SeatCategory seatCategory =
        seatCategoryRepository
            .findByIdWithLock(seatCategoryId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Seat category not found"));

    updateAvailableSeats(
        seatCategory,
        quantity);
  }

  @Transactional(readOnly = true)
  public List<BookingDto.Response> myBookings(
      User user) {

    return bookingRepository
        .findByUserId(user.getId())
        .stream()
        .map(BookingDto.Response::from)
        .toList();
  }

  @Transactional
  public void cancelBookingsForEvent(
      UUID eventId) {

    List<Booking> activeBookings =
        bookingRepository.findByEventIdAndStatusIn(
            eventId,
            List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED));

    for (Booking booking : activeBookings) {

      if (booking.getStatus() == BookingStatus.PENDING) {

        expireBooking(booking);

      } else {

        booking.setStatus(
            BookingStatus.CANCELLED);

        booking.setCancelledAt(
            Instant.now());

        bookingRepository.save(booking);
      }
    }
  }
}

