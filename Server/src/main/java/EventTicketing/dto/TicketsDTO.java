package EventTicketing.dto;

import EventTicketing.model.Ticket;
import EventTicketing.model.TicketAttendee;
import liquibase.exception.CustomPreconditionErrorException;

import java.sql.Time;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public abstract class TicketsDTO {
    
    public record CustomerTicket(String ticketCode,
                                 Date createdAt,
                                 UUID bookingId,
                                 UUID seat,
                                 UUID evnt,
                                 UUID venue,
                                 UUID userOwnerUUID,
                                 Double totalPrice,
                                 String status) {}
    
    public record OrganizerTicketResponse(String ticketCode, List<String> contactInfo, String status) {}
    
    public record AdminTicketResponse(UUID uuid,
                                      String ticketCode,
                                      Date createdAt,
                                      UUID bookingId,
                                      UUID seat,
                                      UUID evnt,
                                      UUID venue,
                                      UUID userOwnerUUID,
                                      Double totalPrice,
                                      String status) {}

    public record OrganizerSpecificEventTickets(String ticketCode,
                                                Date createdAt,
                                                UUID seat,
                                                UUID venue,
                                                UUID userOwnerUUID,
                                                Double totalPrice,
                                                String status) {}

    public record OrganizerSpecificTicket(String ticketCode,
                                                Date createdAt,
                                                UUID seat,
                                                UUID venue,
                                                UUID userOwnerUUID,
                                                Double totalPrice,
                                                String status) {}
    
    public record Request(String ticketCode, Date createdAt,
                          UUID bookingId,
                          UUID seat,
                          UUID evnt,
                          UUID venue,
                          UUID userOwnerUUID,
                          Double totalPrice,
                          String status) {}
}
