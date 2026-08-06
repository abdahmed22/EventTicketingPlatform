package EventTicketing.repository;

import EventTicketing.model.TicketAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketAttendeeRepository  extends JpaRepository<TicketAttendee, UUID> {
    
}
