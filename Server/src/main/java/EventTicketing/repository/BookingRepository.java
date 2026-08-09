package EventTicketing.repository;

import EventTicketing.model.Booking;
import EventTicketing.model.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    // Same locking pattern as SeatCategoryRepository.findByIdWithLock.
    // Needed because confirm()/cancel()/expireBooking() all read-then-write
    // Booking.status, and without a row lock the scheduler can race with a
    // manual confirm/cancel on the same booking (double seat release).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") UUID id);
}