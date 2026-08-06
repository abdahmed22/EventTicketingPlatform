package EventTicketing.repository;

import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.Venue.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface venueRepository extends JpaRepository<Venue, UUID> {
    List<Venue> findByRequestedBy(User requestedBy);

    List<Venue> findByRequestedByAndStatus(User requestedBy, Status status);

    Optional<Venue> findByIdAndRequestedBy(UUID id, User requestedBy);
}
