package EventTicketing.controller;

import EventTicketing.dto.TicketsDTO;
import EventTicketing.model.Ticket;
import EventTicketing.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    // Fix: added `private final` so @RequiredArgsConstructor injects this correctly
    private final TicketService ticketService;

    // -----------------------------------------------------------------------
    // Admin endpoints
    // -----------------------------------------------------------------------

    @GetMapping("/admin/{uuid}")
    public ResponseEntity<TicketsDTO.AdminTicketResponse> getTicket(@PathVariable("uuid") String uuid) {
        Ticket t = ticketService.getTicketForAdmin(UUID.fromString(uuid));
        // Fix: was HttpStatus.FOUND (302). Now returns 200 OK.
        return ResponseEntity.ok(toAdminResponse(t));
    }

    @GetMapping("/admin/CustomerTickets/{customerUUID}")
    // Fix: was List<ResponseEntity<...>>, now ResponseEntity<List<...>>
    public ResponseEntity<List<TicketsDTO.AdminTicketResponse>> getAdminCustomerTickets(
            @PathVariable("customerUUID") UUID customer) {
        List<Ticket> ts = ticketService.getCustomerTickets(customer);
        return ResponseEntity.ok(ts.stream().map(this::toAdminResponse).toList());
    }

    @GetMapping("/admin/EventTickets/{eventUUID}")
    // Fix: was List<ResponseEntity<...>>, now ResponseEntity<List<...>>
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

    // Fix: @PathVariable name now explicitly bound so Spring matches {event_uuid}
    @GetMapping("/organizer/{event_uuid}")
    public ResponseEntity<List<TicketsDTO.OrganizerSpecificEventTickets>> organizerGetEventTickets(
            @PathVariable("event_uuid") UUID eventUuid) {
        var tickets = ticketService.getEventTickets(eventUuid);
        // Fix: was HttpStatus.FOUND (302)
        return ResponseEntity.ok(tickets.stream().map(t -> new TicketsDTO.OrganizerSpecificEventTickets(
                t.getTicketCode(), t.getCreatedAt(), t.getSeat(), t.getVenue(),
                t.getUserOwnerUUID(), t.getTotalPrice(), t.getStatus())).toList());
    }

    // Fix: @PathVariable name now explicitly bound so Spring matches {event_uuid} and {ticket_code}
    @GetMapping("/organizer/{event_uuid}/{ticket_code}")
    public ResponseEntity<TicketsDTO.OrganizerSpecificTicket> organizerGetSpecificTicketInEvent(
            @PathVariable("event_uuid") UUID eventUuid,
            @PathVariable("ticket_code") String ticketCode) {
        var t = ticketService.getTicketForOrganizer(ticketCode, eventUuid);
        // Fix: was HttpStatus.FOUND (302)
        return ResponseEntity.ok(new TicketsDTO.OrganizerSpecificTicket(
                t.getTicketCode(), t.getCreatedAt(), t.getSeat(), t.getVenue(),
                t.getUserOwnerUUID(), t.getTotalPrice(), t.getStatus()));
    }

    // Fix: was `void` and discarded ResponseEntity.ok(...). Now returns the checked-in ticket.
    @PostMapping("/organizer/events/{eventId}/check-in")
    public ResponseEntity<TicketsDTO.OrganizerSpecificTicket> checkIn(
            @PathVariable UUID eventId,
            @RequestBody TicketsDTO.CheckInRequest request) {
        var t = ticketService.checkIn(request.ticketCode(), eventId);
        return ResponseEntity.ok(new TicketsDTO.OrganizerSpecificTicket(
                t.getTicketCode(), t.getCreatedAt(), t.getSeat(), t.getVenue(),
                t.getUserOwnerUUID(), t.getTotalPrice(), t.getStatus()));
    }

    // -----------------------------------------------------------------------
    // Customer endpoints
    // -----------------------------------------------------------------------

    // Fix: was @RequestParam on path variable; now uses @PathVariable. Also was HttpStatus.FOUND.
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<TicketsDTO.CustomerTicket>> getCustomerTickets(
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(ticketService.getCustomerTickets(customerId)
                .stream().map(this::toCustomerTicket).toList());
    }

    // Fix: was "/customer/{ticket_code}{customer}" (missing slash). Now "/customer/{customerId}/{ticket_code}".
    @GetMapping("/customer/{customerId}/{ticket_code}")
    public ResponseEntity<TicketsDTO.CustomerTicket> getCustomerTicketByCode(
            @PathVariable UUID customerId,
            @PathVariable("ticket_code") String ticketCode) {
        var ticket = ticketService.getTicketForCustomer(ticketCode, customerId);
        // Fix: was HttpStatus.FOUND (302)
        return ResponseEntity.ok(toCustomerTicket(ticket));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private TicketsDTO.AdminTicketResponse toAdminResponse(Ticket t) {
        return new TicketsDTO.AdminTicketResponse(t.getUuid(), t.getTicketCode(), t.getCreatedAt(),
                t.getBookingId(), t.getSeat(), t.getEvnt(), t.getVenue(),
                t.getUserOwnerUUID(), t.getTotalPrice(), t.getStatus());
    }

    private TicketsDTO.CustomerTicket toCustomerTicket(Ticket ticket) {
        return new TicketsDTO.CustomerTicket(ticket.getTicketCode(), ticket.getCreatedAt(),
                ticket.getBookingId(), ticket.getSeat(), ticket.getEvnt(), ticket.getVenue(),
                ticket.getUserOwnerUUID(), ticket.getTotalPrice(), ticket.getStatus());
    }
}
