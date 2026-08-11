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

    @Query(value = """
            SELECT DISTINCT e FROM Event e
            LEFT JOIN e.seatCategories sc
            WHERE e.status = :status
                AND (cast(:category as string) IS NULL OR e.category = :category)
                AND (cast(:venueId as java.util.UUID) IS NULL OR e.venue.id = :venueId)
                AND (cast(:organizerId as java.util.UUID) IS NULL OR e.organizer.id = :organizerId)
                AND (cast(:dateFrom as java.time.LocalDate) IS NULL OR e.eventDate >= :dateFrom)
                AND (cast(:dateTo as java.time.LocalDate) IS NULL OR e.eventDate <= :dateTo)
                AND (cast(:minPrice as java.math.BigDecimal) IS NULL OR sc.price >= :minPrice)
                AND (cast(:maxPrice as java.math.BigDecimal) IS NULL OR sc.price <= :maxPrice)
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e.id) FROM Event e
            LEFT JOIN e.seatCategories sc
            WHERE e.status = :status
                AND (cast(:category as string) IS NULL OR e.category = :category)
                AND (cast(:venueId as java.util.UUID) IS NULL OR e.venue.id = :venueId)
                AND (cast(:organizerId as java.util.UUID) IS NULL OR e.organizer.id = :organizerId)
                AND (cast(:dateFrom as java.time.LocalDate) IS NULL OR e.eventDate >= :dateFrom)
                AND (cast(:dateTo as java.time.LocalDate) IS NULL OR e.eventDate <= :dateTo)
                AND (cast(:minPrice as java.math.BigDecimal) IS NULL OR sc.price >= :minPrice)
                AND (cast(:maxPrice as java.math.BigDecimal) IS NULL OR sc.price <= :maxPrice)
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
    @Query(value = """
            SELECT DISTINCT e FROM Event e
            LEFT JOIN e.seatCategories sc
            WHERE (cast(:status as string) IS NULL OR e.status = :status)
                AND (cast(:category as string) IS NULL OR e.category = :category)
                AND (cast(:venueId as java.util.UUID) IS NULL OR e.venue.id = :venueId)
                AND (cast(:organizerId as java.util.UUID) IS NULL OR e.organizer.id = :organizerId)
                AND (cast(:dateFrom as java.time.LocalDate) IS NULL OR e.eventDate >= :dateFrom)
                AND (cast(:dateTo as java.time.LocalDate) IS NULL OR e.eventDate <= :dateTo)
                AND (cast(:minPrice as java.math.BigDecimal) IS NULL OR sc.price >= :minPrice)
                AND (cast(:maxPrice as java.math.BigDecimal) IS NULL OR sc.price <= :maxPrice)
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e.id) FROM Event e
            LEFT JOIN e.seatCategories sc
            WHERE (cast(:status as string) IS NULL OR e.status = :status)
                AND (cast(:category as string) IS NULL OR e.category = :category)
                AND (cast(:venueId as java.util.UUID) IS NULL OR e.venue.id = :venueId)
                AND (cast(:organizerId as java.util.UUID) IS NULL OR e.organizer.id = :organizerId)
                AND (cast(:dateFrom as java.time.LocalDate) IS NULL OR e.eventDate >= :dateFrom)
                AND (cast(:dateTo as java.time.LocalDate) IS NULL OR e.eventDate <= :dateTo)
                AND (cast(:minPrice as java.math.BigDecimal) IS NULL OR sc.price >= :minPrice)
                AND (cast(:maxPrice as java.math.BigDecimal) IS NULL OR sc.price <= :maxPrice)
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
