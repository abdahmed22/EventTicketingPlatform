package EventTicketing.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Time;
import java.sql.Date;
import java.util.UUID;

@Table(name = "tickets")
@Entity
@RequiredArgsConstructor
@Data
public class Ticket {

    @Id
    @UuidGenerator
    private UUID uuid;

    @Column(name = "ticket_code")
    private String ticketCode;

    @Column(name = "creation_date")
    private String createdAt;

    private UUID bookingId;
    private UUID seat;
    private UUID evnt;
    private UUID venue;
    private UUID userOwnerUUID;

    @Column(name = "total_price")
    private Double totalPrice;

    private String status;
}
