package EventTicketing.service;

import EventTicketing.model.Booking;
import EventTicketing.model.enums.BookingStatus;
import EventTicketing.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Scheduled(fixedRate = 60000)
    public void expirePendingBookings() {

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndExpiresAtBefore(
                        BookingStatus.PENDING,
                        Instant.now()
                );

        for (Booking booking : expiredBookings) {

            // Pass only the id: the entities above were read outside a
            // transaction and are detached, so expireBooking() re-fetches
            // and locks the row itself inside its own transaction. This
            // guards against acting on a stale status if the booking was
            // confirmed/cancelled between this query and now.
            bookingService.expireBooking(booking.getId());
        }
    }
}