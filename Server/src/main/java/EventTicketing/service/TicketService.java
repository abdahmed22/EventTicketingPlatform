package EventTicketing.service;

import EventTicketing.dto.TicketsDTO;
import EventTicketing.model.Ticket;
import EventTicketing.model.TicketAttendee;
import EventTicketing.repository.TicketAttendeeRepository;
import EventTicketing.repository.TicketRepository;
import EventTicketing.repository.bookingRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TicketService {
    private TicketRepository ticketRepository;
    private TicketAttendeeRepository taRepo;
    
    // الإتنيين function الجايين هما مش عندهم وعي خالص بالstatus بتاعت الbooking
    // لإنه في الUX المستخدم بيتعمله ticket بس لما بيعمل Confirm
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
        t.setCreatedAt(new java.util.Date());
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
    
    public Integer editTicket(UUID ticket, TicketsDTO.Request newData) {
        return ticketRepository.updateTicket(ticket, newData.ticketCode(), newData.createdAt(),
                newData.bookingId(), newData.seat(), newData.evnt(), newData.venue(), 
                newData.userOwnerUUID(), newData.totalPrice(), newData.status());
    }
    
    // TODO: لازم البوكينج يخلص علشان اقدر اعمل ده
    public void voidTicket(UUID ticket, UUID booking) {
        throw new NotImplementedException("The BookingRepository Class Needs to be Implement, and specifically the function that gets all booking data");
    }
}
