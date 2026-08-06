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
    @UuidGenerator
    private UUID uuid;

    private UUID ticketId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
}
