package EventTicketing.controller;

import EventTicketing.dto.EventDto;
import EventTicketing.dto.PageResponse;
import EventTicketing.model.Event;
import EventTicketing.model.User;
import EventTicketing.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor // Lombok generates constructor injection for the final field below
public class EventController {

    private final EventService eventService;

    // No authentication required — anyone can browse/view published events.

    /**
     * GET /api/events
     * Paginated, filterable list of PUBLISHED events for public browsing.
     * All filters are optional query params (Spring leaves them null if omitted).
     */
    @GetMapping("/events")
    public ResponseEntity<PageResponse<EventDto.Summary>> browse(
            @RequestParam(required = false) Event.Category category,
            // @DateTimeFormat tells Spring how to parse the query-string date (e.g. "2026-08-11")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(defaultValue = "0") int page,   // pagination: 0-indexed page number
            @RequestParam(defaultValue = "10") int size) { // pagination: results per page
        return ResponseEntity.ok(eventService.browsePublished(
                category, venueId, organizerId, dateFrom, dateTo, minPrice, maxPrice, page, size));
    }

    /**
     * GET /api/events/{id}
     * Fetch a single event by ID — but only if it's PUBLISHED (service layer
     * enforces this, so drafts/canceled events 404 for the public).
     */
    @GetMapping("/events/{id}")
    public ResponseEntity<EventDto.Response> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getPublicById(id));
    }

    // ================== Organizer (§6.4) ==================
    // @AuthenticationPrincipal injects the currently logged-in User (from the security context).
    // Ownership of each event is enforced down in EventService, not here.

    /**
     * GET /api/organizer/events
     * Lists the logged-in organizer's own events, optionally filtered by status/category.
     */
    @GetMapping("/organizer/events")
    public ResponseEntity<PageResponse<EventDto.Summary>> listOrganizerEvents(
            @AuthenticationPrincipal User organizer,
            @RequestParam(required = false) Event.Status status,
            @RequestParam(required = false) Event.Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.listOrganizerEvents(organizer, status, category, page, size));
    }

    /**
     * GET /api/organizer/events/{id}
     * Fetch one of the organizer's own events (any status), by ID.
     */
    @GetMapping("/organizer/events/{id}")
    public ResponseEntity<EventDto.Response> getOrganizerEventDetail(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getOrganizerById(id, organizer));
    }

    /**
     * POST /api/organizer/events
     * Creates a new event (always starts as DRAFT).
     * Valid triggers bean-validation annotations on EventDto.CreateRequest before this runs.
     */
    @PostMapping("/organizer/events")
    public ResponseEntity<EventDto.Response> create(
            @AuthenticationPrincipal User organizer,
            @Valid @RequestBody EventDto.CreateRequest request) {
        // 201 Created is the correct status for a successful POST that creates a new resource
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request, organizer));
    }

    /**
     * PUT /api/organizer/events/{id}
     * Partial update of an owned event (only non-null fields in the request are applied).
     */
    @PutMapping("/organizer/events/{id}")
    public ResponseEntity<EventDto.Response> update(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id,
            @Valid @RequestBody EventDto.UpdateRequest request) {
        return ResponseEntity.ok(eventService.update(id, request, organizer));
    }

    /**
     * POST /api/organizer/events/{id}/publish
     * Transitions an owned event from DRAFT -> PUBLISHED (service enforces the preconditions).
     */
    @PostMapping("/organizer/events/{id}/publish")
    public ResponseEntity<EventDto.Response> publish(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.publish(id, organizer));
    }

    /**
     * POST /api/organizer/events/{id}/cancel
     * Lets an organizer cancel their own event (cascades to cancel its bookings).
     */
    @PostMapping("/organizer/events/{id}/cancel")
    public ResponseEntity<EventDto.Response> organizerCancel(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.cancel(id, organizer));
    }

    // These endpoints are locked down to ADMIN role via Spring Security config


    /**
     * GET /api/admin/events
     * Same as the public browse, but status is optional (null = all statuses,
     * including DRAFT/CANCELLED) and there's no PUBLISHED restriction.
     */
    @GetMapping("/admin/events")
    public ResponseEntity<PageResponse<EventDto.Summary>> adminList(
            @RequestParam(required = false) Event.Status status,
            @RequestParam(required = false) Event.Category category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.adminList(
                status, category, venueId, organizerId, dateFrom, dateTo, minPrice, maxPrice, page, size));
    }

    /**
     * GET /api/admin/events/{id}
     * Fetch any event by ID regardless of status or who owns it.
     */
    @GetMapping("/admin/events/{id}")
    public ResponseEntity<EventDto.Response> adminDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.adminGetById(id));
    }

    /**
     * PUT /api/admin/events/{id}
     * Admin partial update — same pattern as organizer update(), but also allows
     * directly overriding status (bypassing the normal DRAFT->PUBLISHED->CANCELLED
     * transition rules) and skips the venue-approval check.
     */
    @PutMapping("/admin/events/{id}")
    public ResponseEntity<EventDto.Response> adminUpdate(
            @PathVariable UUID id,
            @Valid @RequestBody EventDto.AdminUpdateRequest request) {
        return ResponseEntity.ok(eventService.adminUpdate(id, request));
    }

    /**
     * DELETE /api/admin/events/{id}
     * Hard-deletes an event. Service layer blocks this if the event has existing
     * bookings (use cancel instead in that case). 204 No Content on success.
     */
    @DeleteMapping("/admin/events/{id}")
    public ResponseEntity<Void> adminDelete(@PathVariable UUID id) {
        eventService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/admin/events/{id}/cancel
     * Admin-triggered cancel — reuses the same EventService.cancel() as the organizer
     * endpoint. Note: cancel() internally calls findOwnedEvent(), which allows this
     * through because the caller has the ADMIN role (bypasses the ownership check).
     */
    @PostMapping("/admin/events/{id}/cancel")
    public ResponseEntity<EventDto.Response> adminCancel(
            @AuthenticationPrincipal User admin,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.cancel(id, admin));
    }
}