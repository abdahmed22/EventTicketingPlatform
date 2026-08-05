package EventTicketing.service;

import EventTicketing.dto.TicketsDTO;
import EventTicketing.model.Ticket;
import EventTicketing.model.TicketAttendee;
import EventTicketing.repository.TicketAttendeeRepository;
import EventTicketing.repository.TicketRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.Date;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TicketService {
    private TicketRepository ticketRepository;
    private TicketAttendeeRepository taRepo;
    
    public void generateTicket(String ticketCode, Date creationDate, Time creationTime,
                            UUID bookingId,
                            UUID seat,
                            UUID evnt,
                            UUID venue,
                            UUID userOwnerUUID,
                            Double totalPrice,
                            String status) {
        Ticket t = new Ticket();
        
        t.setTicketCode(null);
        t.setCreatedAt(new Date().toString());
        t.setBookingId(bookingId);
        t.setSeat(seat);
        t.setEvnt(evnt);
        t.setVenue(venue);
        t.setUserOwnerUUID(userOwnerUUID);
        t.setTotalPrice(totalPrice);
        t.setStatus(status);
        
        ticketRepository.save(t);
    }
    
    public void createTicketAttendee(UUID ticket, UUID attendee) {
        TicketAttendee ticketAttendee = new TicketAttendee();
        ticketAttendee.setTicketId(ticket);
        ticketAttendee.setCustomerId(attendee);
        
        taRepo.save(ticketAttendee);
    }
}
