package EventTicketing.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "ticket_attendee")
public class TicketAttendee {
        
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;
    
    private UUID ticketUUID;

    @Column(name = "customer_id", nullable = false)
    private UUID customerUUId;
}
