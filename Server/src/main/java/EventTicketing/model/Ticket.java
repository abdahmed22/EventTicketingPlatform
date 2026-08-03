package EventTicketing.model;


import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;

import java.sql.Time;
import java.sql.Date;
import java.util.UUID;

@Table(name = "tickets")
@Entity
@RequiredArgsConstructor

public class Ticket {
    
    @Id
    private UUID uuid;
    
    @Column(name = "ticket_code")
    private String ticketCode;

    @Column(name = "creation_date")
    private Date creationDate;

    @Column(name = "creation_time")
    private Time creationTime;

    private UUID bookingId;
    private UUID seat;
    private UUID evnt;
    private UUID venue;
    private UUID userOwnerUUID;

    @Column(name = "total_price")
    private Double totalPrice;

    private String status;
}
