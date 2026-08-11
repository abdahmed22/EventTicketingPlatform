package EventTicketing.service;

import EventTicketing.dto.TicketsDTO;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.Event;
import EventTicketing.model.Ticket;
import EventTicketing.model.TicketAttendee;
import EventTicketing.model.enums.TicketStatus;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.TicketAttendeeRepository;
import EventTicketing.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TicketService {

    // URL-safe alphanumeric characters only — no special chars that would break path variables
    private static final String TICKET_CODE_SOURCE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 27;

    private TicketRepository ticketRepository;
    private TicketAttendeeRepository taRepo;
    private EventRepository eventRepository;

    // -----------------------------------------------------------------------
    // Code generation
    // -----------------------------------------------------------------------

    private String makeTicketCode() {
        String code;
        do {
            code = randomCode();
        } while (ticketRepository.existsByTicketCode(code));
        return code;
    }

    private String randomCode() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(TICKET_CODE_SOURCE.charAt(rng.nextInt(TICKET_CODE_SOURCE.length())));
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Ticket generation — called by BookingService when a booking is CONFIRMED
    // -----------------------------------------------------------------------

    /**
     * Creates one ticket record for a confirmed booking.
     * The caller MUST ensure the booking is already in CONFIRMED state
     * before invoking this method — no re-lookup is performed here so
     * this can safely be called within the same transaction as confirm().
     *
     * @param bookingId      the confirmed booking UUID
     * @param seatCategoryId the seat category UUID (used as the "seat" FK)
     * @param evnt           the event UUID
     * @param venue          the venue UUID
     * @param userOwnerUUID  the customer who owns this ticket
     * @param totalPrice     price for this individual ticket (booking total / quantity)
     */
    @Transactional
    public Ticket generateTicket(UUID bookingId,
                                 UUID seatCategoryId,
                                 UUID evnt,
                                 UUID venue,
                                 UUID userOwnerUUID,
                                 BigDecimal totalPrice) {

        Ticket t = Ticket.builder()
                .ticketCode(makeTicketCode())
                .bookingId(bookingId)
                .seat(seatCategoryId)
                .evnt(evnt)
                .venue(venue)
                .userOwnerUUID(userOwnerUUID)
                .totalPrice(totalPrice)
                .status(TicketStatus.ISSUED)
                .build();

        return ticketRepository.save(t);
    }

    // -----------------------------------------------------------------------
    // Attendee linking
    // -----------------------------------------------------------------------

    @Transactional
    public void createTicketAttendee(UUID ticketId, UUID attendeeId) {
        TicketAttendee ticketAttendee = new TicketAttendee();
        ticketAttendee.setTicketId(ticketId);
        ticketAttendee.setCustomerId(attendeeId);
        taRepo.save(ticketAttendee);
    }

    // -----------------------------------------------------------------------
    // Retrieval
    // -----------------------------------------------------------------------

    public List<Ticket> getCustomerTickets(UUID customerUUID) {
        return ticketRepository.getAllTicketsMadeByCustomer(customerUUID);
    }

    public Ticket getTicketForCustomer(String ticketCode, UUID customerUUID) {
        return ticketRepository.getTicketByOwnerUUID(ticketCode, customerUUID);
    }

    public Ticket getTicketForOrganizer(String ticketCode, UUID eventUUID) {
        return ticketRepository.getTicketForOrganizerForOneEvent(ticketCode, eventUUID);
    }

    public List<Ticket> getEventTickets(UUID eventUUID) {
        return ticketRepository.getEventTickets(eventUUID);
    }

    public Ticket getTicketForAdmin(UUID ticketUUID) {
        return ticketRepository.getTicketByUUID(ticketUUID);
    }

    // -----------------------------------------------------------------------
    // Check-in
    // -----------------------------------------------------------------------

    @Transactional
    public Ticket checkIn(String ticketCode, UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        Ticket ticket = ticketRepository.getTicketForOrganizerForOneEvent(ticketCode, event.getId());
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket not found for event");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot check in a cancelled ticket");
        }
        if (ticket.getStatus() == TicketStatus.CHECKED_IN) {
            throw new IllegalStateException("Ticket already checked in");
        }

        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticket.setCheckedInAt(Instant.now());
        return ticketRepository.save(ticket);
    }

    // -----------------------------------------------------------------------
    // Admin operations
    // -----------------------------------------------------------------------

    public Integer editTicket(UUID ticketId, TicketsDTO.Request newData) {
        return ticketRepository.updateTicket(ticketId, newData.ticketCode(), newData.createdAt(),
                newData.bookingId(), newData.seat(), newData.evnt(), newData.venue(),
                newData.userOwnerUUID(), newData.totalPrice(), newData.status());
    }

    @Transactional
    public void cancelTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.getTicketByUUID(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket not found: " + ticketId);
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already cancelled");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
    }

    /**
     * Voids (cancels) all ISSUED tickets for a given booking — used when a booking
     * is cancelled after tickets have already been issued.
     */
    @Transactional
    public void voidTicketsForBooking(UUID bookingId) {
        ticketRepository.updateStatusByBookingId(bookingId, TicketStatus.ISSUED, TicketStatus.CANCELLED);
    }
}
