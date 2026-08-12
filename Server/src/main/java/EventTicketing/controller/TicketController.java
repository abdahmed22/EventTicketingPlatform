package EventTicketing.controller;

import EventTicketing.dto.TicketsDTO;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.Ticket;
import EventTicketing.model.User;
import EventTicketing.model.enums.UserRole;
import EventTicketing.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // -----------------------------------------------------------------------
    // Admin endpoints
    // -----------------------------------------------------------------------

    @GetMapping("/admin/{uuid}")
    public ResponseEntity<TicketsDTO.AdminTicketResponse> getTicket(@PathVariable("uuid") String uuid) {
        Ticket t = ticketService.getTicketForAdmin(UUID.fromString(uuid));
        if (t == null) {
            throw new ResourceNotFoundException("Ticket not found: " + uuid);
        }
        return ResponseEntity.ok(toAdminResponse(t));
    }

    @GetMapping("/admin/CustomerTickets/{customerUUID}")
    public ResponseEntity<List<TicketsDTO.AdminTicketResponse>> getAdminCustomerTickets(
            @PathVariable("customerUUID") UUID customer) {
        List<Ticket> ts = ticketService.getCustomerTickets(customer);
        return ResponseEntity.ok(ts.stream().map(this::toAdminResponse).toList());
    }

    @GetMapping("/admin/EventTickets/{eventUUID}")
    public ResponseEntity<List<TicketsDTO.AdminTicketResponse>> getEventTickets(
            @PathVariable("eventUUID") UUID event) {
        List<Ticket> ts = ticketService.getEventTickets(event);
        return ResponseEntity.ok(ts.stream().map(this::toAdminResponse).toList());
    }

    @PostMapping("/admin/EditTicket/{ticketUUID}")
    public ResponseEntity<Integer> editTicket(@PathVariable("ticketUUID") UUID ticket,
                                              @RequestBody TicketsDTO.Request newValues) {
        return ResponseEntity.accepted().body(ticketService.editTicket(ticket, newValues));
    }

    @PostMapping("/admin/{ticketId}/cancel")
    public ResponseEntity<Void> cancelTicket(@PathVariable UUID ticketId) {
        ticketService.cancelTicket(ticketId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Organizer endpoints
    // -----------------------------------------------------------------------

    @GetMapping("/organizer/{event_uuid}")
    public ResponseEntity<List<TicketsDTO.OrganizerSpecificEventTickets>> organizerGetEventTickets(
            @PathVariable("event_uuid") UUID eventUuid) {
        var tickets = ticketService.getEventTickets(eventUuid);
        return ResponseEntity.ok(tickets.stream().map(this::toOrganizerEventTicket).toList());
    }

    @GetMapping("/organizer/{event_uuid}/{ticket_code}")
    public ResponseEntity<TicketsDTO.OrganizerSpecificTicket> organizerGetSpecificTicketInEvent(
            @PathVariable("event_uuid") UUID eventUuid,
            @PathVariable("ticket_code") String ticketCode) {
        var t = ticketService.getTicketForOrganizer(ticketCode, eventUuid);
        if (t == null) {
            throw new ResourceNotFoundException("Ticket not found for event");
        }
        return ResponseEntity.ok(toOrganizerTicket(t));
    }

    @PostMapping("/organizer/events/{eventId}/check-in")
    public ResponseEntity<TicketsDTO.OrganizerSpecificTicket> checkIn(
            @PathVariable UUID eventId,
            @RequestBody TicketsDTO.CheckInRequest request) {
        var t = ticketService.checkIn(request.ticketCode(), eventId);
        return ResponseEntity.ok(toOrganizerTicket(t));
    }

    // -----------------------------------------------------------------------
    // Customer endpoints
    // -----------------------------------------------------------------------

    /**
     * Preferred list endpoint: uses the JWT principal, so the frontend does not
     * have to pass (and cannot spoof) a customer UUID.
     */
    @GetMapping("/my")
    public ResponseEntity<List<TicketsDTO.CustomerTicket>> getMyTickets(
            @AuthenticationPrincipal User requester) {
        requireUser(requester);
        return ResponseEntity.ok(ticketService.getCustomerTickets(requester.getId())
                .stream().map(this::toCustomerTicket).toList());
    }

    @GetMapping("/my/booking/{bookingId}")
    public ResponseEntity<TicketsDTO.CustomerTicket> getMyTicketForBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User requester) {
        requireUser(requester);
        Ticket ticket = ticketService.getTicketForBooking(bookingId);
        if (ticket == null) {
            throw new ResourceNotFoundException(
                    "No ticket has been issued for this booking yet");
        }
        assertSelfOrAdmin(ticket.getUserOwnerUUID(), requester);
        return ResponseEntity.ok(toCustomerTicket(ticket));
    }

    @GetMapping("/my/booking/{bookingId}/pdf")
    public ResponseEntity<byte[]> downloadMyTicketPdf(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User requester) {
        requireUser(requester);
        byte[] pdf = ticketService.generateTicketPdf(bookingId, requester);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("ticket-" + bookingId + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }

    // Ownership is enforced below: a customer may only fetch their own tickets
    // (an admin may fetch any customer's tickets).
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<TicketsDTO.CustomerTicket>> getCustomerTickets(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal User requester) {
        assertSelfOrAdmin(customerId, requester);
        return ResponseEntity.ok(ticketService.getCustomerTickets(customerId)
                .stream().map(this::toCustomerTicket).toList());
    }

    @GetMapping("/customer/{customerId}/{ticket_code}")
    public ResponseEntity<TicketsDTO.CustomerTicket> getCustomerTicketByCode(
            @PathVariable UUID customerId,
            @PathVariable("ticket_code") String ticketCode,
            @AuthenticationPrincipal User requester) {
        assertSelfOrAdmin(customerId, requester);
        var ticket = ticketService.getTicketForCustomer(ticketCode, customerId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket not found");
        }
        return ResponseEntity.ok(toCustomerTicket(ticket));
    }

    // Download the PDF ticket for a booking. One ticket now covers the
    // whole booking, so this is the single downloadable artifact for it.
    // Only the booking's owner or an admin may download it.
    @GetMapping("/customer/{customerId}/booking/{bookingId}/pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(
            @PathVariable UUID customerId,
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User requester) {
        assertSelfOrAdmin(customerId, requester);

        byte[] pdf = ticketService.generateTicketPdf(bookingId, requester);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("ticket-" + bookingId + ".pdf")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void requireUser(User requester) {
        if (requester == null) {
            throw new ForbiddenActionException("Authentication is required");
        }
    }

    private void assertSelfOrAdmin(UUID customerId, User requester) {
        requireUser(requester);
        boolean isSelf = requester.getId() != null && requester.getId().equals(customerId);
        boolean isAdmin = requester.getRole() == UserRole.ADMIN;
        if (!isSelf && !isAdmin) {
            throw new ForbiddenActionException("You may only access your own tickets");
        }
    }

    private TicketsDTO.AdminTicketResponse toAdminResponse(Ticket t) {
        return new TicketsDTO.AdminTicketResponse(t.getUuid(), t.getTicketCode(), t.getCreatedAt(),
                t.getBookingId(), t.getSeat(), t.getEvnt(), t.getVenue(),
                t.getUserOwnerUUID(), t.getQuantity(), t.getTotalPrice(), t.getStatus());
    }

    private TicketsDTO.CustomerTicket toCustomerTicket(Ticket ticket) {
        return new TicketsDTO.CustomerTicket(ticket.getTicketCode(), ticket.getCreatedAt(),
                ticket.getBookingId(), ticket.getSeat(), ticket.getEvnt(), ticket.getVenue(),
                ticket.getUserOwnerUUID(), ticket.getQuantity(), ticket.getTotalPrice(), ticket.getStatus());
    }

    private TicketsDTO.OrganizerSpecificEventTickets toOrganizerEventTicket(Ticket t) {
        return new TicketsDTO.OrganizerSpecificEventTickets(t.getTicketCode(), t.getCreatedAt(), t.getSeat(),
                t.getVenue(), t.getUserOwnerUUID(), t.getQuantity(), t.getTotalPrice(), t.getStatus());
    }

    private TicketsDTO.OrganizerSpecificTicket toOrganizerTicket(Ticket t) {
        return new TicketsDTO.OrganizerSpecificTicket(t.getTicketCode(), t.getCreatedAt(), t.getSeat(),
                t.getVenue(), t.getUserOwnerUUID(), t.getQuantity(), t.getTotalPrice(), t.getStatus());
    }
}
