package EventTicketing.repository;

import EventTicketing.model.OrganizerApplication;
import EventTicketing.model.enums.OrganizerApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface organizerApplicationRepository extends JpaRepository<OrganizerApplication, UUID> {

    Optional<OrganizerApplication> findByEmail(String email);

    Optional<OrganizerApplication> findByEmailAndStatus(String email, OrganizerApplicationStatus status);

    List<OrganizerApplication> findByStatus(OrganizerApplicationStatus status);

    boolean existsByEmailAndStatus(String email, OrganizerApplicationStatus status);

    @Query("SELECT a FROM OrganizerApplication a WHERE a.status = :status AND (a.email = :identifier OR a.phone = :identifier)")
    Optional<OrganizerApplication> findByStatusAndIdentifier(
            @Param("status") OrganizerApplicationStatus status,
            @Param("identifier") String identifier);
}