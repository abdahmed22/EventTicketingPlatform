package EventTicketing.controller;

import EventTicketing.dto.TicketsDTO;
import EventTicketing.model.Ticket;
import EventTicketing.model.User;
import EventTicketing.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    
    TicketService ticketService;
    
    @GetMapping("/admin/{uuid}")
    public ResponseEntity<TicketsDTO.AdminTicketResponse> getTicket(@PathVariable("uuid") String uuid) {
        Ticket t = ticketService.getTicketForAdmin(UUID.fromString(uuid));
        return new ResponseEntity<>(new TicketsDTO.AdminTicketResponse(t.getUuid(),t.getTicketCode(), t.getCreatedAt(), t.getBookingId(), t.getSeat(), t.getEvnt(), t.getVenue(), t.getUserOwnerUUID(), t.getTotalPrice(), t.getStatus()),  HttpStatus.FOUND);
    }
    
    @GetMapping("/admin/CustomerTickets/{customerUUID}")
    public List<ResponseEntity<TicketsDTO.AdminTicketResponse>> getCustomerTickets(@PathVariable("customerUUID") UUID customer) {
        List<Ticket> ts = ticketService.getCustomerTickets(customer);
        return ts.stream().map(
                (ticket) -> {
                    return ResponseEntity.ok(new TicketsDTO.AdminTicketResponse(ticket.getUuid(),
                            ticket.getTicketCode(),
                            ticket.getCreatedAt(),
                            ticket.getBookingId(),
                            ticket.getSeat(),
                            ticket.getEvnt(),
                            ticket.getVenue(),
                            ticket.getUserOwnerUUID(),
                            ticket.getTotalPrice(),
                            ticket.getStatus()));
                }
                
        ).toList();
    }
    
    
    @GetMapping("/admin/EventTickets/{eventUUID}")
    public List<ResponseEntity<TicketsDTO.AdminTicketResponse>> getEventTickets(@PathVariable("eventUUID") UUID event) {
        List<Ticket> ts = ticketService.getEventTickets(event);
        return ts.stream().map(
                (ticket) -> {
                    return ResponseEntity.ok(new TicketsDTO.AdminTicketResponse(ticket.getUuid(),
                            ticket.getTicketCode(),
                            ticket.getCreatedAt(),
                            ticket.getBookingId(),
                            ticket.getSeat(),
                            ticket.getEvnt(),
                            ticket.getVenue(),
                            ticket.getUserOwnerUUID(),
                            ticket.getTotalPrice(),
                            ticket.getStatus()));
                }

        ).toList();
    }
    
    @PostMapping("/admin/EditTicket/{ticketUUID}")
    public ResponseEntity<Integer> editTicket(@PathVariable("ticketUUID") UUID ticket, @RequestBody TicketsDTO.Request newValues) {
        return new ResponseEntity<>(ticketService.editTicket(ticket, newValues), HttpStatus.ACCEPTED);
    }

    @PostMapping("/admin/{ticketId}/cancel")
    public void cancelTicket(@PathVariable UUID ticketId) {
        ticketService.cancelTicket(ticketId);
    }
    
    @GetMapping("/organizer/{event_uuid}")
    public ResponseEntity<List<TicketsDTO.OrganizerSpecificEventTickets>> organizerGetEventTickets(@PathVariable UUID evnt) {
        var tickets = ticketService.getEventTickets(evnt);
        return new ResponseEntity<>(
                tickets.stream().map(
                        (t) -> {
                            return new TicketsDTO.OrganizerSpecificEventTickets(t.getTicketCode(),
                                    t.getCreatedAt(), t.getSeat(), t.getVenue(), t.getUserOwnerUUID(),
                                    t.getTotalPrice(), t.getStatus());
                        }
                ).toList(), HttpStatus.FOUND
        );
    }

    @GetMapping("/organizer/{event_uuid}/{ticket_code}")
    public ResponseEntity<TicketsDTO.OrganizerSpecificTicket> organizerGetSpecificTicketInEvent(@PathVariable UUID event, 
                                                                                                @PathVariable String ticket_code) {
        var t = ticketService.getTicketForOrganizer(ticket_code, event);
        return new ResponseEntity<>(
                new TicketsDTO.OrganizerSpecificTicket(t.getTicketCode(),
                                    t.getCreatedAt(), t.getSeat(), t.getVenue(), t.getUserOwnerUUID(),
                                    t.getTotalPrice(), t.getStatus())
                ,HttpStatus.FOUND
        );
    }

    @PostMapping("/organizer/events/{eventId}/check-in")
    public void checkIn(
            @PathVariable UUID eventId,
            @RequestBody TicketsDTO.CheckInRequest request
            ) {
        ResponseEntity.ok(ticketService.checkIn(request.ticketCode(), eventId));
    }
    
    @GetMapping("/customer/{customer}")
    public ResponseEntity<List<TicketsDTO.CustomerTicket>> getTicketsAcrossEvents(@RequestParam UUID customer) {
        return new ResponseEntity<>(
                ticketService.getCustomerTickets(customer).stream().map(
                        (ticket) -> new TicketsDTO.CustomerTicket(
                                ticket.getTicketCode(),
                                ticket.getCreatedAt(),
                                ticket.getBookingId(),
                                ticket.getSeat(),
                                ticket.getEvnt(),
                                ticket.getVenue(),
                                ticket.getUserOwnerUUID(),
                                ticket.getTotalPrice(),
                                ticket.getStatus()
                        )
                ).toList(), HttpStatus.FOUND
        );
    }

    @GetMapping("/customer/{ticket_code}{customer}")
    public ResponseEntity<TicketsDTO.CustomerTicket> getTicketsAcrossEvents(@PathVariable String ticket_code,
                                                                            @PathVariable UUID customer) {
        var ticket = ticketService.getTicketForCustomer(ticket_code, customer);
        return new ResponseEntity<>(new TicketsDTO.CustomerTicket(
                                ticket.getTicketCode(),
                                ticket.getCreatedAt(),
                                ticket.getBookingId(),
                                ticket.getSeat(),
                                ticket.getEvnt(),
                                ticket.getVenue(),
                                ticket.getUserOwnerUUID(),
                                ticket.getTotalPrice(),
                                ticket.getStatus()
                        ) , HttpStatus.FOUND
        );
    }
    
}
