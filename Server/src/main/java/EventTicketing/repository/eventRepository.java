package EventTicketing.repository;

import EventTicketing.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface eventRepository extends JpaRepository<Event, UUID> {

}