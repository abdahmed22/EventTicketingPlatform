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
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.BookingRepository;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.SeatCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  // @Lazy breaks the circular dependency: BookingService <-> TicketService
  @Lazy
  private final TicketService ticketService;

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

    // Check seat limit per person (seatingCapacity)
    if (seatCategory.getSeatingCapacity() != null && seatCategory.getSeatingCapacity() > 0) {
      if (request.quantity() > seatCategory.getSeatingCapacity()) {
        throw new IllegalStateException(
            "Requested quantity exceeds seat limit per person (" + seatCategory.getSeatingCapacity() + ")");
      }
      List<Booking> activeBookings = bookingRepository.findByUserIdAndSeatCategoryIdAndStatusIn(
          user.getId(),
          seatCategory.getId(),
          List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
      int alreadyBooked = activeBookings.stream().mapToInt(Booking::getQuantity).sum();
      if (alreadyBooked + request.quantity() > seatCategory.getSeatingCapacity()) {
        throw new IllegalStateException(
            "Seat limit per person (" + seatCategory.getSeatingCapacity() + ") reached for this category");
      }
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

    // Lock the booking row for the duration of this transaction so a
    // concurrent cancel/expire on the same booking blocks until this
    // transaction commits, instead of racing on a stale in-memory copy.
    Booking booking = bookingRepository
        .findByIdWithLock(bookingId)
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

      // booking is already locked in this transaction, so expire it
      // directly instead of re-acquiring the lock.
      expireLockedBooking(booking);

      throw new IllegalStateException(
          "Booking reservation expired");
    }

    booking.setStatus(
        BookingStatus.CONFIRMED);

    booking.setConfirmedAt(
        Instant.now());

    Booking savedBooking =
        bookingRepository.save(booking);

    // Issue the single ticket covering every seat in this booking now that
    // it is confirmed. One ticket per booking - not one per seat.
    ticketService.generateTicketForBooking(savedBooking);

    return BookingDto.Response.from(
        savedBooking);
  }

  @Transactional
  public BookingDto.Response cancel(
      UUID bookingId,
      User user) {

    // Lock the booking row before inspecting/changing its status so this
    // cannot race with a concurrent confirm or scheduler expiration.
    Booking booking = bookingRepository
        .findByIdWithLock(bookingId)
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

    if (booking.getStatus() == BookingStatus.CONFIRMED) {
      ticketService.voidTicketsForBooking(booking.getId());
    }

    booking.setStatus(
        BookingStatus.CANCELLED);

    booking.setCancelledAt(
        Instant.now());

    Booking savedBooking =
        bookingRepository.save(booking);

    // If this booking had already been confirmed, its ticket was already
    // issued — void it too so a cancelled booking never leaves a valid
    // downloadable/checkable ticket behind.
    ticketService.voidTicketForBooking(savedBooking.getId());

    return BookingDto.Response.from(
        savedBooking);
  }

  /**
   * Entry point used by the scheduler (and any other caller) to expire a
   * PENDING booking by id. Re-fetches and locks the booking row inside its
   * own transaction so this cannot race with a concurrent confirm/cancel on
   * the same booking - whichever operation acquires the row lock first wins,
   * and the other observes the up-to-date status once it acquires the lock.
   */
  @Transactional
  public void expireBooking(UUID bookingId) {

    bookingRepository
        .findByIdWithLock(bookingId)
        .ifPresent(this::expireLockedBooking);
  }

  /**
   * Expires a booking that the caller has ALREADY locked (via
   * findByIdWithLock) within the current transaction. Never call this with
   * an entity that was not fetched under a pessimistic write lock in the
   * current transaction.
   */
  private void expireLockedBooking(Booking booking) {

    // Idempotent / safe against races: if the booking was already
    // confirmed, cancelled, or expired by the time this transaction
    // acquired the lock, do nothing so seats are never released twice.
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

  @Transactional
  public List<BookingDto.Response> myBookings(
      User user) {

    // Auto-expire any pending bookings whose reservation timer has expired
    List<Booking> userBookings = bookingRepository.findByUserId(user.getId());
    Instant now = Instant.now();
    for (Booking booking : userBookings) {
      if (booking.getStatus() == BookingStatus.PENDING
          && booking.getExpiresAt() != null
          && booking.getExpiresAt().isBefore(now)) {
        expireBooking(booking.getId());
      }
    }

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

    for (Booking summary : activeBookings) {

      // Re-fetch with a lock: a user could be concurrently
      // confirming/cancelling/expiring the same booking while an
      // organizer cancels the whole event.
      Booking booking = bookingRepository
          .findByIdWithLock(summary.getId())
          .orElse(null);

      if (booking == null
          || booking.getStatus() == BookingStatus.CANCELLED
          || booking.getStatus() == BookingStatus.EXPIRED) {

        // Already resolved by another concurrent operation - skip so
        // seats are never released twice.
        continue;
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

      if (booking.getStatus() == BookingStatus.PENDING) {

        booking.setStatus(
            BookingStatus.EXPIRED);

      } else {

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
          ticketService.voidTicketsForBooking(booking.getId());
        }

        booking.setStatus(
            BookingStatus.CANCELLED);

        booking.setCancelledAt(
            Instant.now());

        // The booking was CONFIRMED, so a ticket had already been issued -
        // void it along with the booking.
        ticketService.voidTicketForBooking(booking.getId());
      }

      bookingRepository.save(booking);
    }
  }

  @Transactional(readOnly = true)
  public List<BookingDto.Response> getEventBookings(UUID eventId, User organizer) {
    Event event = eventRepository.findById(eventId)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

    if (organizer.getRole() != UserRole.ADMIN && !event.getOrganizer().getId().equals(organizer.getId())) {
      throw new ForbiddenActionException("You do not have permission to view bookings for this event");
    }

    return bookingRepository.findByEventId(eventId).stream()
        .map(BookingDto.Response::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BookingDto.Response> getAllBookingsForAdmin(User admin) {
    if (admin.getRole() != UserRole.ADMIN) {
      throw new ForbiddenActionException("Only admin users may perform this action");
    }

    return bookingRepository.findAll().stream()
        .map(BookingDto.Response::from)
        .toList();
  }
}