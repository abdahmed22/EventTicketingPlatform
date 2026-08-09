package EventTicketing.model;


import EventTicketing.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Table(name = "tickets")
@Entity
@RequiredArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class Ticket {

    @Id
    @UuidGenerator
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "ticket_code", unique = true)
    private String ticketCode;

    @Column(name = "creation_date")
    private Instant createdAt;

    @JoinColumn(name = "booking_id", nullable = false)
    private UUID bookingId;

    @JoinColumn(name = "seat", nullable = false)
    private UUID seat;
    
    @JoinColumn(name = "evnt", nullable = false)
    private UUID evnt;
    @JoinColumn(name = "venue", nullable = false)
    private UUID venue;
    @JoinColumn(name = "userOwnerUUID", nullable = false)
    private UUID userOwnerUUID;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "status")
    private TicketStatus status;
    
    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null)
            this.status = TicketStatus.ISSUED;
    }
    
}
