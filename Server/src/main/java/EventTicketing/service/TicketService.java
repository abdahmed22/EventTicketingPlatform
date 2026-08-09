package EventTicketing.service;

import EventTicketing.dto.TicketsDTO;
import EventTicketing.model.Event;
import EventTicketing.model.Ticket;
import EventTicketing.model.TicketAttendee;
import EventTicketing.model.enums.BookingStatus;
import EventTicketing.model.enums.TicketStatus;
import EventTicketing.repository.BookingRepository;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.TicketAttendeeRepository;
import EventTicketing.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.procedure.ParameterMisuseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Time;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TicketService {

    private static final String TICKET_CODE__GENERATION_SOURCE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*(){}[];:";
    private static final int CODE_LENGTH = 27;
    
    private TicketRepository ticketRepository;
    private TicketAttendeeRepository taRepo;
    private EventRepository eventRepository;
    private BookingRepository bookingRepository;
    
    // الإتنيين function الجايين هما مش عندهم وعي خالص بالstatus بتاعت الbooking
    // لإنه في الUX المستخدم بيتعمله ticket بس لما بيعمل Confirm
    private String makeTicketCode() {
        String code;
        do {
            code = randomCode();
        } while (ticketRepository.existsByTicketCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(TICKET_CODE__GENERATION_SOURCE.charAt(new SecureRandom().nextInt(TICKET_CODE__GENERATION_SOURCE.length())));
        }
        return sb.toString();
    }
    
    @Transactional
    public void generateTicket(String ticketCode, Date creationDate, Time creationTime,
                            UUID bookingId,
                            UUID seat,
                            UUID evnt,
                            UUID venue,
                            UUID userOwnerUUID,
                            BigDecimal totalPrice,
                            TicketStatus status) {
        if (bookingRepository.getReferenceById(bookingId).getStatus() != BookingStatus.CONFIRMED) {
            throw new ParameterMisuseException(
                    "Tickets can only be issued for a CONFIRMED booking"); 
        }
        
        Ticket t = new Ticket();
        
        t.setTicketCode(makeTicketCode());
        t.setCreatedAt(Instant.now());
        t.setBookingId(bookingId);
        t.setSeat(seat);
        t.setEvnt(evnt);
        t.setVenue(venue);
        t.setUserOwnerUUID(userOwnerUUID);
        t.setTotalPrice(totalPrice);
        t.setStatus(status);
        
        ticketRepository.save(t);
    }
    
    @Transactional
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

    @Transactional
    public Ticket checkIn(String ticketCode, UUID eventId) {
        Event event = eventRepository.getReferenceById(eventId);
        Ticket ticket = ticketRepository.getTicketForOrganizerForOneEvent(ticketCode, event.getId());

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ParameterMisuseException("Cannot check in a cancelled ticket");
        }
        if (ticket.getStatus() == TicketStatus.CHECKED_IN) {
            throw new ParameterMisuseException("Ticket already checked in");
        }

        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticket.setCheckedInAt(Instant.now());
        return ticketRepository.save(ticket);
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
    
    public void cancelTicket(UUID t) {
        Ticket ticket = ticketRepository.getTicketByUUID(t);
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ParameterMisuseException("Ticket is already cancelled");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
    }
    
    // TODO: لازم البوكينج يخلص علشان اقدر اعمل ده
    public void voidTicket(UUID ticket, UUID booking) {
        throw new NotImplementedException("The BookingRepository Class Needs to be Implement, and specifically the function that gets all booking data");
    }
}
