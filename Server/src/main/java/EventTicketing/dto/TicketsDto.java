package EventTicketing.dto;

import EventTicketing.model.TicketAttendee;
import liquibase.exception.CustomPreconditionErrorException;

import java.sql.Time;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public abstract class TicketsDTO {
    
    public record CustomerTicket(String eventName, Date eventDate, Time eventTIme, String venueName, String ticketCode, Double price, String status, List<TicketAttendee> ticketAttendees) {}
    public record OrganizerTicketResponse(String ticketCode, List<String> contactInfo, String status) {}
    public record AdminTicketResponse(UUID uuid,
                                      String ticketCode,
                                      Date creationDate,
                                      Time creationTime,
                                      UUID bookingId,
                                      UUID seat,
                                      UUID evnt,
                                      UUID venue,
                                      UUID userOwnerUUID,
                                      Double totalPrice,
                                      String status) {}
    
    public record Request(String ticketCode, Date creationDate, Time creationTime,
                          UUID bookingId,
                          UUID seat,
                          UUID evnt,
                          UUID venue,
                          UUID userOwnerUUID,
                          Double totalPrice,
                          String status) {}
}
