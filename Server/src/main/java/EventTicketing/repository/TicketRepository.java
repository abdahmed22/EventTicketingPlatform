package EventTicketing.repository;

import EventTicketing.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // ديه للUser والadmin
    @Query("SELECT t FROM Ticket t WHERE t.uuid = :uuid")
    Optional<Ticket> getTicketByUUID(@Param("uuid") UUID uuid);

    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode")
    Optional<List<Ticket>> getEventTickets(@Param("ticket_code") String ticketCode);


    @Query("SELECT t FROM Ticket t WHERE t.userOwnerUUID = :owner_id")
    Optional<List<Ticket>> getAllTicketsMadeByCustomer(@Param("owner_id") UUID owner_id);

    // علشان يعمل insert بتستخدم الsave() fuction الي بيقدمها الJpaRepository

    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode AND t.userOwnerUUID = :owner")
    Optional<Ticket> getTicketByOwnerUUID(@Param("ticket_code") String ticketCode, @Param("owner_id") UUID owner);

    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode AND t.evnt = :evnt")
    Optional<List<Ticket>> getEventTickets(@Param("ticket_code") String ticketCode, @Param("owner_id") UUID evnt);


}
