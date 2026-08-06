package EventTicketing.repository;

import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface seatCategoryRepository extends JpaRepository<SeatCategory, UUID> {
    List<SeatCategory> findByEvent(Event event);

    List<SeatCategory> findByEventId(UUID eventId);

    Optional<SeatCategory> findByIdAndEventId(UUID id, UUID eventId);
}
