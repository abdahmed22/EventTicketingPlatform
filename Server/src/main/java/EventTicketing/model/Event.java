package EventTicketing.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    // Stored separately as requested
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false, insertable = false, updatable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false, insertable = false, updatable = false)
    private User organizer;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SeatCategory> seatCategories = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Category {
        MUSIC,
        SPORTS,
        CONFERENCE,
        THEATRE,
        OTHER
    }

    public enum Status {
        DRAFT,
        PUBLISHED,
        CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = Status.DRAFT;
        }
        if (venueId == null && venue != null) {
            venueId = venue.getId();
        }
        if (organizerId == null && organizer != null) {
            organizerId = organizer.getId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (venueId == null && venue != null) {
            venueId = venue.getId();
        }
        if (organizerId == null && organizer != null) {
            organizerId = organizer.getId();
        }
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
        if (venue != null) {
            this.venueId = venue.getId();
        }
    }

    public void setOrganizer(User organizer) {
        this.organizer = organizer;
        if (organizer != null) {
            this.organizerId = organizer.getId();
        }
    }

    // Bridge for eventService: combines eventDate + eventTime into a single LocalDateTime
    public LocalDateTime getDateTime() {
        if (eventDate == null) return null;
        LocalTime time = (eventTime != null) ? eventTime : LocalTime.MIDNIGHT;
        return LocalDateTime.of(eventDate, time);
    }

    public void setDateTime(LocalDateTime dateTime) {
        if (dateTime != null) {
            this.eventDate = dateTime.toLocalDate();
            this.eventTime = dateTime.toLocalTime();
        } else {
            this.eventDate = null;
            this.eventTime = null;
        }
    }
}