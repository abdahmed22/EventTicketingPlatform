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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeatCategoryController {

    private final SeatCategoryService seatCategoryService;

    @GetMapping("/events/{eventId}/seat-categories")
    public ResponseEntity<List<SeatCategoryDto.Summary>> listByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(seatCategoryService.listByEvent(eventId));
    }

    @PostMapping("/organizer/events/{eventId}/seat-categories")
    public ResponseEntity<SeatCategoryDto.Response> create(@AuthenticationPrincipal User organizer,
            @PathVariable UUID eventId,
            @Valid @RequestBody SeatCategoryDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatCategoryService.create(eventId, organizer, request));
    }

    @PutMapping("/organizer/seat-categories/{id}")
    public ResponseEntity<SeatCategoryDto.Response> update(@AuthenticationPrincipal User organizer,
            @PathVariable UUID id,
            @Valid @RequestBody SeatCategoryDto.UpdateRequest request) {
        return ResponseEntity.ok(seatCategoryService.update(id, organizer, request));
    }
}
