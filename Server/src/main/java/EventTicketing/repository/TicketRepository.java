package EventTicketing.repository;

import EventTicketing.model.Ticket;
import EventTicketing.model.enums.TicketStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // ديه للUser والadmin
    @Query("SELECT t FROM Ticket t WHERE t.uuid = :uuid")
    Ticket getTicketByUUID(@Param("uuid") UUID uuid);

    @Query("SELECT t FROM Ticket t WHERE t.evnt = :event_UUID")
    List<Ticket> getEventTickets(@Param("event_uuid") UUID event_uuid);

    Ticket findByTicketCode(String ticketCode);
    
    @Query("SELECT t FROM Ticket t WHERE t.userOwnerUUID = :owner_id")
    List<Ticket> getAllTicketsMadeByCustomer(@Param("owner_id") UUID owner_id);

    // علشان يعمل insert بتستخدم الsave() fuction الي بيقدمها الJpaRepository

    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode AND t.userOwnerUUID = :owner")
    Ticket getTicketByOwnerUUID(@Param("ticket_code") String ticketCode, @Param("owner_id") UUID owner);

    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode AND t.evnt = :evnt")
    Ticket getTicketForOrganizerForOneEvent(@Param("ticket_code") String ticketCode, @Param("owner_id") UUID evnt);

    @Modifying
    @Transactional
    @Query("UPDATE Ticket t SET " +
            "t.ticketCode = :ticketCode, " +
            "t.createdAt = :createdAt, " +
            "t.bookingId = :bookingId, " +
            "t.seat = :seat, " +
            "t.evnt = :evnt, " +
            "t.venue = :venue, " +
            "t.userOwnerUUID = :userOwnerUUID, " +
            "t.totalPrice = :totalPrice, " +
            "t.status = :status " +
            "WHERE t.uuid = :ticketId")
    Integer updateTicket(@Param("ticketId") UUID ticketId,
                      @Param("ticketCode") String ticketCode,
                      @Param("createdAt") Instant createdAt,
                      @Param("bookingId") UUID bookingId,
                      @Param("seat") UUID seat,
                      @Param("evnt") UUID evnt,
                      @Param("venue") UUID venue,
                      @Param("userOwnerUUID") UUID userOwnerUUID,
                      @Param("totalPrice") BigDecimal totalPrice,
                      @Param("status") TicketStatus status);

    @Modifying
    @Query("UPDATE Ticket t SET t.status = :status WHERE t.bookingId = :bookingId AND t.status == :status")
    Integer updateStatusByBookingId(@Param("bookingId") UUID bookingId,
                                @Param("status") TicketStatus status);

   boolean existsByTicketCode(String ticketCode);
}
