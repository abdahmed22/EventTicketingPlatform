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
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ---- Public browsing (§6.2) ----

    @GetMapping("/events")
    public ResponseEntity<PageResponse<EventDto.Summary>> browse(
            @RequestParam(required = false) Event.Category category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.browsePublished(
                category, venueId, organizerId, dateFrom, dateTo, minPrice, maxPrice, page, size));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventDto.Response> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getPublicById(id));
    }

    // ---- Organizer (§6.4) ----

    @GetMapping("/organizer/events")
    public ResponseEntity<PageResponse<EventDto.Summary>> listOrganizerEvents(
            @AuthenticationPrincipal User organizer,
            @RequestParam(required = false) Event.Status status,
            @RequestParam(required = false) Event.Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.listOrganizerEvents(organizer, status, category, page, size));
    }

    @GetMapping("/organizer/events/{id}")
    public ResponseEntity<EventDto.Response> getOrganizerEventDetail(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getOrganizerById(id, organizer));
    }

    @PostMapping("/organizer/events")
    public ResponseEntity<EventDto.Response> create(
            @AuthenticationPrincipal User organizer,
            @Valid @RequestBody EventDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request, organizer));
    }

    @PutMapping("/organizer/events/{id}")
    public ResponseEntity<EventDto.Response> update(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id,
            @Valid @RequestBody EventDto.UpdateRequest request) {
        return ResponseEntity.ok(eventService.update(id, request, organizer));
    }

    @PostMapping("/organizer/events/{id}/publish")
    public ResponseEntity<EventDto.Response> publish(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.publish(id, organizer));
    }

    @PostMapping("/organizer/events/{id}/cancel")
    public ResponseEntity<EventDto.Response> organizerCancel(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.cancel(id, organizer));
    }

    // ---- Admin (§6.5): full CRUD, no ownership restriction (§4.4 decision #2) ----

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

    @GetMapping("/admin/events/{id}")
    public ResponseEntity<EventDto.Response> adminDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.adminGetById(id));
    }

    @PutMapping("/admin/events/{id}")
    public ResponseEntity<EventDto.Response> adminUpdate(
            @PathVariable UUID id,
            @Valid @RequestBody EventDto.AdminUpdateRequest request) {
        return ResponseEntity.ok(eventService.adminUpdate(id, request));
    }

    @DeleteMapping("/admin/events/{id}")
    public ResponseEntity<Void> adminDelete(@PathVariable UUID id) {
        eventService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/events/{id}/cancel")
    public ResponseEntity<EventDto.Response> adminCancel(
            @AuthenticationPrincipal User admin,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.cancel(id, admin));
    }
}