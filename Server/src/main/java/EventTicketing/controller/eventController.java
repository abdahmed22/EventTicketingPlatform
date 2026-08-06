package EventTicketing.controller;

import EventTicketing.dto.PageResponse;
import EventTicketing.dto.eventDto;
import EventTicketing.model.Event;
import EventTicketing.model.User;
import EventTicketing.service.eventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class eventController {

    private final eventService eventService;

    @GetMapping("/events")
    public ResponseEntity<PageResponse<eventDto.Summary>> browsePublished(
            @RequestParam(required = false) Event.Category category,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.browsePublished(category, venueId, organizerId, dateFrom, dateTo,
                minPrice, maxPrice, page, size));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<eventDto.Response> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getPublicById(id));
    }

    @PostMapping("/organizer/events")
    public ResponseEntity<eventDto.Response> create(@AuthenticationPrincipal User organizer,
            @RequestBody eventDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request, organizer));
    }

    @PutMapping("/organizer/events/{id}")
    public ResponseEntity<eventDto.Response> update(@AuthenticationPrincipal User organizer,
            @PathVariable UUID id,
            @RequestBody eventDto.UpdateRequest request) {
        return ResponseEntity.ok(eventService.update(id, request, organizer));
    }

    @PostMapping("/organizer/events/{id}/publish")
    public ResponseEntity<eventDto.Response> publish(@AuthenticationPrincipal User organizer,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.publish(id, organizer));
    }

    @PostMapping("/organizer/events/{id}/cancel")
    public ResponseEntity<eventDto.Response> cancel(@AuthenticationPrincipal User organizer,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.cancel(id, organizer));
    }
}
