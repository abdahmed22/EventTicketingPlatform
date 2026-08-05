package EventTicketing.service;

import EventTicketing.dto.bookingDto;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.model.Booking;
import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.User;
import EventTicketing.model.enums.BookingStatus;
import EventTicketing.repository.BookingRepository;
import EventTicketing.repository.eventRepository;
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
public class bookingService {

    private final BookingRepository bookingRepository;
    private final eventRepository eventRepository;
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
    public bookingDto.Response reserve(
            User user,
            bookingDto.CreateRequest request
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


        return bookingDto.Response.from(savedBooking);
    }



    @Transactional
    public bookingDto.Response confirm(
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


        booking.setStatus(
                BookingStatus.CONFIRMED
        );

        booking.setConfirmedAt(
                Instant.now()
        );


        return bookingDto.Response.from(
                bookingRepository.save(booking)
        );
    }



    @Transactional
    public bookingDto.Response cancel(
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
                == BookingStatus.CANCELLED) {


            throw new IllegalStateException(
                    "Booking already cancelled");
        }



        // return seats
        updateAvailableSeats(
                booking.getSeatCategory(),
                booking.getQuantity()
        );



        booking.setStatus(
                BookingStatus.CANCELLED
        );


        booking.setCancelledAt(
                Instant.now()
        );



        return bookingDto.Response.from(
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





    public List<bookingDto.Response> myBookings(
            User user
    ) {


        return bookingRepository
                .findByUserId(user.getId())

                .stream()

                .map(bookingDto.Response::from)

                .toList();
    }
}