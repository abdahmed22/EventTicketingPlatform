package EventTicketing.dto;

import EventTicketing.model.Ticket;
import EventTicketing.model.TicketAttendee;
import EventTicketing.model.enums.TicketStatus;
import liquibase.exception.CustomPreconditionErrorException;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public abstract class TicketsDTO {

    public record Attendee(UUID ticket, UUID customer) { }
    
    public record CustomerTicket(String ticketCode,
                                 Instant createdAt,
                                 UUID bookingId,
                                 UUID seat,
                                 UUID evnt,
                                 UUID venue,
                                 UUID userOwnerUUID,
                                 BigDecimal totalPrice,
                                 TicketStatus status
                                 ) {}

    public record CustomerTicketWithAttendees(String ticketCode,
                                 Instant createdAt,
                                 UUID bookingId,
                                 UUID seat,
                                 UUID evnt,
                                 UUID venue,
                                 UUID userOwnerUUID,
                                 BigDecimal totalPrice,
                                 TicketStatus status,
                                 List<TicketAttendee> attendees) {}
    
    public record AdminTicketResponse(UUID uuid,
                                      String ticketCode,
                                      Instant createdAt,
                                      UUID bookingId,
                                      UUID seat,
                                      UUID evnt,
                                      UUID venue,
                                      UUID userOwnerUUID,
                                      BigDecimal totalPrice,
                                      TicketStatus status) {}

    public record OrganizerSpecificEventTickets(String ticketCode,
                                                Instant createdAt,
                                                UUID seat,
                                                UUID venue,
                                                UUID userOwnerUUID,
                                                BigDecimal totalPrice,
                                                TicketStatus status) {}

    public record OrganizerSpecificTicket(String ticketCode,
                                                Instant createdAt,
                                                UUID seat,
                                                UUID venue,
                                                UUID userOwnerUUID,
                                                BigDecimal totalPrice,
                                                TicketStatus status) {}
    
    public record Request(String ticketCode, Instant createdAt,
                          UUID bookingId,
                          UUID seat,
                          UUID evnt,
                          UUID venue,
                          UUID userOwnerUUID,
                          BigDecimal totalPrice,
                          TicketStatus status) {}

    public record RequestWithAttendees(String ticketCode, Instant createdAt,
                          UUID bookingId,
                          UUID seat,
                          UUID evnt,
                          UUID venue,
                          UUID userOwnerUUID,
                          BigDecimal totalPrice,
                          TicketStatus status,
                          List<Attendee> attendees) {}
    
    public record CheckInRequest(String ticketCode) {}
}
