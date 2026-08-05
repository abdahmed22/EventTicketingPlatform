package EventTicketing.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
public class Ticket {

    @Id
    @UuidGenerator
    private UUID uuid;

    @Column(name = "ticket_code")
    private String ticketCode;

    @Column(name = "creation_date")
    private String createdAt;

    @Column(name = "booking_id")
    private UUID bookingId;

    private UUID seat;

    @Column(name = "event_id")
    private UUID evnt;

    @Column(name = "venue_id")
    private UUID venue;

    @Column(name = "user_owner_uuid")
    private UUID userOwnerUUID;

    @Column(name = "total_price")
    private Double totalPrice;

    private String status;
}