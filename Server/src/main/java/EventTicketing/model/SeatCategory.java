package EventTicketing.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "seat_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatCategory {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "seating_capacity", nullable = false)
    @Builder.Default
    private Integer seatingCapacity = 1;
}