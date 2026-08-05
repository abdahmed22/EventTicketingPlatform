package EventTicketing.controller;

import EventTicketing.dto.PageResponse;
import EventTicketing.dto.eventDto;
import EventTicketing.service.eventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class eventController {

    private final eventService eventService;

    @GetMapping("/events")
    public ResponseEntity<PageResponse<eventDto.Summary>> browsePublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.browsePublished(page, size));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<eventDto.Response> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PostMapping("/organizer/events")
    public ResponseEntity<eventDto.Response> create(@RequestBody eventDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @PutMapping("/organizer/events/{id}")
    public ResponseEntity<eventDto.Response> update(@PathVariable UUID id,
            @RequestBody eventDto.UpdateRequest request) {
        return ResponseEntity.ok(eventService.update(id, request));
    }

    @PostMapping("/organizer/events/{id}/publish")
    public ResponseEntity<eventDto.Response> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.publish(id));
    }

    @PostMapping("/organizer/events/{id}/cancel")
    public ResponseEntity<eventDto.Response> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.cancel(id));
    }
}
