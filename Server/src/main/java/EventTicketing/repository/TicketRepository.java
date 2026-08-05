package EventTicketing.repository;

import EventTicketing.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByUuid(UUID uuid);

    List<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByUserOwnerUUID(UUID ownerId);

    Optional<Ticket> findByTicketCodeAndUserOwnerUUID(
            String ticketCode,
            UUID ownerId);

    List<Ticket> findByTicketCodeAndEvnt(
            String ticketCode,
            UUID eventId);

}

