package EventTicketing.repository;

import EventTicketing.model.Booking;
import EventTicketing.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserId(UUID userId);

    List<Booking> findByStatusAndExpiresAtBefore(
            BookingStatus status,
            Instant time
    );
    List<Booking> findByEventIdAndStatusIn(UUID eventId, List<BookingStatus> statuses);
    boolean existsByEventId(UUID eventId);
}