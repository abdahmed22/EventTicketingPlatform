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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // Get a single ticket by its primary-key UUID (used by admin & customer)
    @Query("SELECT t FROM Ticket t WHERE t.uuid = :uuid")
    Ticket getTicketByUUID(@Param("uuid") UUID uuid);

    // One ticket per booking - used to fetch/generate the single ticket
    // that covers every seat in a booking (e.g. for PDF download).
    Optional<Ticket> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    // Fix: JPQL param name was :event_UUID (uppercase) but @Param said "event_uuid"
    @Query("SELECT t FROM Ticket t WHERE t.evnt = :eventUuid")
    List<Ticket> getEventTickets(@Param("eventUuid") UUID eventUuid);

    Ticket findByTicketCode(String ticketCode);

    @Query("SELECT t FROM Ticket t WHERE t.userOwnerUUID = :ownerId")
    List<Ticket> getAllTicketsMadeByCustomer(@Param("ownerId") UUID ownerId);

    // Fix: JPQL used :ticketCode and :owner but @Param said "ticket_code" and "owner_id"
    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode AND t.userOwnerUUID = :ownerId")
    Ticket getTicketByOwnerUUID(@Param("ticketCode") String ticketCode, @Param("ownerId") UUID ownerId);

    // Fix: JPQL used :ticketCode and :evnt but @Param said "ticket_code" and "owner_id"
    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode AND t.evnt = :eventUuid")
    Ticket getTicketForOrganizerForOneEvent(@Param("ticketCode") String ticketCode, @Param("eventUuid") UUID eventUuid);

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
            "t.quantity = :quantity, " +
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
                      @Param("quantity") Integer quantity,
                      @Param("totalPrice") BigDecimal totalPrice,
                      @Param("status") TicketStatus status);

    // Fix: was comparing status = :newStatus in WHERE (no-op). Now accepts separate oldStatus / newStatus.
    @Modifying
    @Transactional
    @Query("UPDATE Ticket t SET t.status = :newStatus WHERE t.bookingId = :bookingId AND t.status = :oldStatus")
    Integer updateStatusByBookingId(@Param("bookingId") UUID bookingId,
                                    @Param("oldStatus") TicketStatus oldStatus,
                                    @Param("newStatus") TicketStatus newStatus);

    boolean existsByTicketCode(String ticketCode);
}
