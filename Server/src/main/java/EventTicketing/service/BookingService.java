package EventTicketing.service;

import EventTicketing.dto.BookingDto;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.exception.ForbiddenActionException;
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


    // Single method responsible for all availableSeats changes
    @Transactional
    public void updateAvailableSeats(SeatCategory seatCategory, int amount) {

        seatCategory.setAvailableSeats(
                seatCategory.getAvailableSeats() + amount
        );

        seatCategoryRepository.save(seatCategory);
    }


    @Transactional
    public BookingDto.Response reserve(
            User user,
            BookingDto.CreateRequest request
    ) {

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found"));


        SeatCategory seatCategory =
                seatCategoryRepository.findByIdWithLock(request.seatCategoryId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Seat category not found"));


        if (seatCategory.getAvailableSeats() < request.quantity()) {

            throw new IllegalStateException(
                    "Not enough seats available");
        }


        BigDecimal totalPrice =
                seatCategory.getPrice()
                        .multiply(
                                BigDecimal.valueOf(request.quantity())
                        );


        // decrease seats
        updateAvailableSeats(
                seatCategory,
                -request.quantity()
        );


        Booking booking = Booking.builder()
                .user(user)
                .event(event)
                .seatCategory(seatCategory)
                .quantity(request.quantity())
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(
                        Instant.now()
                                .plus(5, ChronoUnit.MINUTES)
                )
                .build();


        Booking savedBooking =
                bookingRepository.save(booking);


        return BookingDto.Response.from(savedBooking);
    }



    @Transactional
    public BookingDto.Response confirm(
            UUID bookingId,
            User user
    ) {

        Booking booking =
                bookingRepository.findById(bookingId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found"));


        if (!booking.getUser().getId()
                .equals(user.getId())) {

            throw new ForbiddenActionException(
                    "You cannot confirm this booking");
        }


        if (booking.getStatus()
                != BookingStatus.PENDING) {

            throw new IllegalStateException(
                    "Booking cannot be confirmed");
        }


        if (Instant.now().isAfter(booking.getExpiresAt())) {
            throw new IllegalStateException("Booking reservation expired");
        }


        booking.setStatus(
                BookingStatus.CONFIRMED
        );

        booking.setConfirmedAt(
                Instant.now()
        );


        return BookingDto.Response.from(
                bookingRepository.save(booking)
        );
    }



    @Transactional
    public BookingDto.Response cancel(
            UUID bookingId,
            User user
    ) {


        Booking booking =
                bookingRepository.findById(bookingId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found"));



        if (!booking.getUser().getId()
                .equals(user.getId())) {


            throw new ForbiddenActionException(
                    "You cannot cancel this booking");
        }



        if (booking.getStatus()
                == BookingStatus.CANCELLED
                || booking.getStatus()
                == BookingStatus.EXPIRED) {


            throw new IllegalStateException(
                    "Booking is already inactive");
        }



        // Lock SeatCategory before returning seats to avoid race conditions
        SeatCategory seatCategory =
                seatCategoryRepository.findByIdWithLock(booking.getSeatCategory().getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Seat category not found"));

        // return seats
        updateAvailableSeats(
                seatCategory,
                booking.getQuantity()
        );



        booking.setStatus(
                BookingStatus.CANCELLED
        );


        booking.setCancelledAt(
                Instant.now()
        );



        return BookingDto.Response.from(
                bookingRepository.save(booking)
        );
    }




    // Used by Scheduler
    @Transactional
    public void releaseSeats(
            UUID seatCategoryId,
            int quantity
    ) {


        SeatCategory seatCategory =
                seatCategoryRepository
                        .findByIdWithLock(seatCategoryId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Seat category not found"));



        updateAvailableSeats(
                seatCategory,
                quantity
        );
    }




    public List<BookingDto.Response> myBookings(
            User user
    ) {


        return bookingRepository
                .findByUserId(user.getId())

                .stream()

                .map(BookingDto.Response::from)

                .toList();
    }

    @Transactional
    public void cancelBookingsForEvent(UUID eventId) {
        List<Booking> activeBookings = bookingRepository.findByEventIdAndStatusIn(
                eventId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        for (Booking booking : activeBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(Instant.now());
            bookingRepository.save(booking);
        }
    }
}