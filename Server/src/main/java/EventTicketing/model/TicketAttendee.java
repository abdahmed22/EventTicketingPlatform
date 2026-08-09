package EventTicketing.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "ticket_attendee")
@Data
public class TicketAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(name = "ticket_id", nullable = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "customer_id", nullable = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UUID customerId;
}
