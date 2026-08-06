package EventTicketing.repository;

import EventTicketing.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface venueRepository extends JpaRepository<Venue, UUID> {
}