package EventTicketing.repository;

import EventTicketing.model.Event;
import EventTicketing.model.Event.Category;
import EventTicketing.model.Event.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByStatus(Status status, Pageable pageable);

    java.util.Optional<Event> findByIdAndStatus(UUID id, Status status);

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN e.seatCategories sc
            WHERE e.status = :status
                AND (:category IS NULL OR e.category = :category)
                AND (:venueId IS NULL OR e.venue.id = :venueId)
                AND (:organizerId IS NULL OR e.organizer.id = :organizerId)
                AND (:dateFrom IS NULL OR e.eventDate >= :dateFrom)
                AND (:dateTo IS NULL OR e.eventDate <= :dateTo)
                AND (:minPrice IS NULL OR sc.price >= :minPrice)
                AND (:maxPrice IS NULL OR sc.price <= :maxPrice)
            """)
    Page<Event> findFilteredPublished(
            @Param("status") Status status,
            @Param("category") Category category,
            @Param("venueId") UUID venueId,
            @Param("organizerId") UUID organizerId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    // Admin-facing variant: status is optional (null = every status), used by
    // GET /api/admin/events so admins can see DRAFT/CANCELLED events too, not just PUBLISHED.
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN e.seatCategories sc
            WHERE (:status IS NULL OR e.status = :status)
                AND (:category IS NULL OR e.category = :category)
                AND (:venueId IS NULL OR e.venue.id = :venueId)
                AND (:organizerId IS NULL OR e.organizer.id = :organizerId)
                AND (:dateFrom IS NULL OR e.eventDate >= :dateFrom)
                AND (:dateTo IS NULL OR e.eventDate <= :dateTo)
                AND (:minPrice IS NULL OR sc.price >= :minPrice)
                AND (:maxPrice IS NULL OR sc.price <= :maxPrice)
            """)
    Page<Event> findFilteredForAdmin(
            @Param("status") Status status,
            @Param("category") Category category,
            @Param("venueId") UUID venueId,
            @Param("organizerId") UUID organizerId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    boolean existsByVenueId(UUID venueId);
}