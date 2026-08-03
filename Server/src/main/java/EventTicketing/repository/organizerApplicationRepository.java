package EventTicketing.repository;

import EventTicketing.model.OrganizerApplication;
import EventTicketing.model.enums.OrganizerApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface organizerApplicationRepository extends JpaRepository<OrganizerApplication, UUID> {

    Optional<OrganizerApplication> findByEmail(String email);

    Optional<OrganizerApplication> findByEmailAndStatus(String email, OrganizerApplicationStatus status);

    List<OrganizerApplication> findByStatus(OrganizerApplicationStatus status);

    boolean existsByEmailAndStatus(String email, OrganizerApplicationStatus status);
}