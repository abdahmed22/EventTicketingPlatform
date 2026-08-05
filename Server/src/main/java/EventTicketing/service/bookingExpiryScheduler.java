package EventTicketing.service;

import EventTicketing.model.Booking;
import EventTicketing.model.enums.BookingStatus;
import EventTicketing.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;

    private final bookingService bookingService;


    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingBookings() {

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndExpiresAtBefore(
                        BookingStatus.PENDING,
                        Instant.now()
                );


        for (Booking booking : expiredBookings) {


            bookingService.releaseSeats(
                    booking.getSeatCategory().getId(),
                    booking.getQuantity()
            );


            booking.setStatus(
                    BookingStatus.CANCELLED
            );


            booking.setCancelledAt(
                    Instant.now()
            );


            bookingRepository.save(booking);
        }
    }
}