package EventTicketing.repository;

import EventTicketing.model.Booking;
import EventTicketing.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface bookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserId(UUID userId);

    List<Booking> findByStatusAndExpiresAtBefore(
            BookingStatus status,
            Instant time
    );
}