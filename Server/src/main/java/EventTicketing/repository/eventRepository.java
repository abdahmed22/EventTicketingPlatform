package EventTicketing.repository;

import EventTicketing.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface eventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByStatus(Event.Status status, Pageable pageable);

    Page<Event> findByStatusAndCategory(Event.Status status, Event.Category category, Pageable pageable);
}