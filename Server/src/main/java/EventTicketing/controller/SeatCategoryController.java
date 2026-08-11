package EventTicketing.controller;

import EventTicketing.dto.SeatCategoryDto;
import EventTicketing.model.User;
import EventTicketing.service.SeatCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for managing seat categories.
 * Public listing lives under /api/events, while create/update are
 * organizer-only actions nested under /api/organizer.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeatCategoryController {

    private final SeatCategoryService seatCategoryService;

    /**
     * GET /api/events/{eventId}/seat-categories
     * Public endpoint — anyone can view the seat categories for an event.
     * No authentication principal required.
     */
    @GetMapping("/events/{eventId}/seat-categories")
    public ResponseEntity<List<SeatCategoryDto.Summary>> listByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(seatCategoryService.listByEvent(eventId));
    }

    /**
     * POST /api/organizer/events/{eventId}/seat-categories
     * Creates a new seat category under the given event.
     * @AuthenticationPrincipal injects the currently logged-in user, which the
     * service uses to verify they own the event (or are an ADMIN).
     * @Valid triggers bean validation on the request body before this method runs.
     * Responds 201 Created on success.
     */
    @PostMapping("/organizer/events/{eventId}/seat-categories")
    public ResponseEntity<SeatCategoryDto.Response> create(@AuthenticationPrincipal User organizer,
                                                           @PathVariable UUID eventId,
                                                           @Valid @RequestBody SeatCategoryDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatCategoryService.create(eventId, organizer, request));
    }

    /**
     * PUT /api/organizer/seat-categories/{id}
     * Updates an existing seat category by its own id (not scoped under the event
     * in the URL, since the id alone is sufficient to look it up).
     * Supports partial updates — fields left null in the request body are
     * left unchanged by the service layer.
     * Responds 200 OK on success.
     */
    @PutMapping("/organizer/seat-categories/{id}")
    public ResponseEntity<SeatCategoryDto.Response> update(@AuthenticationPrincipal User organizer,
                                                           @PathVariable UUID id,
                                                           @Valid @RequestBody SeatCategoryDto.UpdateRequest request) {
        return ResponseEntity.ok(seatCategoryService.update(id, organizer, request));
    }
}