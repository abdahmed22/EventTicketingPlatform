package EventTicketing.service;

import EventTicketing.model.Booking;
import EventTicketing.model.enums.BookingStatus;
import EventTicketing.repository.BookingRepository;
import EventTicketing.repository.SeatCategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final SeatCategoryRepository seatCategoryRepository;


    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingBookings() {

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndExpiresAtBefore(
                        BookingStatus.PENDING,
                        Instant.now()
                );


        for (Booking booking : expiredBookings) {

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(Instant.now());


            booking.getSeatCategory()
                    .setAvailableSeats(
                            booking.getSeatCategory().getAvailableSeats()
                                    + booking.getQuantity()
                    );


            seatCategoryRepository.save(
                    booking.getSeatCategory()
            );

            bookingRepository.save(booking);
        }
    }
}