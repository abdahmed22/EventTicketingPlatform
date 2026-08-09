package EventTicketing.repository;

import EventTicketing.model.User;
import EventTicketing.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    List<Venue> findByRequestedBy(User organizer);

    List<Venue> findByStatus(Venue.Status status);
}
