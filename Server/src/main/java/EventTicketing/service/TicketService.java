package EventTicketing.service;

import EventTicketing.dto.TicketsDTO;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.Booking;
import EventTicketing.model.Event;
import EventTicketing.model.Ticket;
import EventTicketing.model.TicketAttendee;
import EventTicketing.model.User;
import EventTicketing.model.enums.TicketStatus;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.BookingRepository;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.TicketAttendeeRepository;
import EventTicketing.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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
    private BookingRepository bookingRepository;
    private TicketPdfService ticketPdfService;

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
     * Issues the single ticket for a confirmed booking. One ticket now
     * covers every seat purchased in the booking (see `quantity`) instead
     * of a separate ticket per seat.
     * <p>
     * The caller MUST ensure the booking is already in CONFIRMED state
     * before invoking this method — no re-lookup is performed here so
     * this can safely be called within the same transaction as confirm().
     * It is also idempotent: if a ticket already exists for this booking
     * (e.g. a retried request), the existing ticket is returned unchanged
     * rather than creating a duplicate.
     *
     * @param booking the confirmed booking to issue a ticket for
     */
    @Transactional
    public Ticket generateTicketForBooking(Booking booking) {
        return ticketRepository.findByBookingId(booking.getId())
                .orElseGet(() -> {
                    Ticket t = Ticket.builder()
                            .ticketCode(makeTicketCode())
                            .bookingId(booking.getId())
                            .seat(booking.getSeatCategory().getId())
                            .evnt(booking.getEvent().getId())
                            .venue(booking.getEvent().getVenueId())
                            .userOwnerUUID(booking.getUser().getId())
                            .quantity(booking.getQuantity())
                            .totalPrice(booking.getTotalPrice())
                            .status(TicketStatus.ISSUED)
                            .build();

                    return ticketRepository.save(t);
                });
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

    /**
     * Fetches the single ticket issued for a booking. Returns null if the
     * booking has not been confirmed yet (no ticket issued).
     */
    public Ticket getTicketForBooking(UUID bookingId) {
        return ticketRepository.findByBookingId(bookingId).orElse(null);
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
                newData.userOwnerUUID(), newData.quantity(), newData.totalPrice(), newData.status());
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
     * Voids (cancels) the ticket for a given booking — used when a booking
     * is cancelled after its ticket has already been issued. A no-op if no
     * ticket was ever issued for the booking (e.g. it was still PENDING).
     */
    @Transactional
    public void voidTicketForBooking(UUID bookingId) {
        ticketRepository.updateStatusByBookingId(bookingId, TicketStatus.ISSUED, TicketStatus.CANCELLED);
    }

    // -----------------------------------------------------------------------
    // PDF download
    // -----------------------------------------------------------------------

    /**
     * Generates the downloadable PDF for the ticket issued to a booking.
     * Only the booking's owner (or an admin) may download it.
     */
    @Transactional
    public byte[] generateTicketPdf(UUID bookingId, User requester) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        boolean isOwner = booking.getUser().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenActionException("You do not have access to this booking's ticket");
        }

        Ticket ticket = ticketRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No ticket has been issued for this booking yet. Tickets are issued once a booking is confirmed."));

        return ticketPdfService.render(ticket, booking);
    }
}
