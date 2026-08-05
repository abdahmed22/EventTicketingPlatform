package EventTicketing.service;

import EventTicketing.model.Booking;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.enums.BookingStatus;
import EventTicketing.repository.bookingRepository;
import EventTicketing.repository.seatCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class bookingExpiryScheduler {

    private final bookingRepository bookingRepository;
    private final seatCategoryRepository seatCategoryRepository;


    @Scheduled(fixedRate = 60000) // every 1 minute
    @Transactional
    public void expirePendingBookings() {

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndExpiresAtBefore(
                        BookingStatus.PENDING,
                        Instant.now()
                );


        for (Booking booking : expiredBookings) {

            SeatCategory seatCategory = booking.getSeatCategory();

            seatCategory.setAvailableSeats(
                    seatCategory.getAvailableSeats()
                            + booking.getQuantity()
            );

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(Instant.now());

            seatCategoryRepository.save(seatCategory);
            bookingRepository.save(booking);
        }
    }
}