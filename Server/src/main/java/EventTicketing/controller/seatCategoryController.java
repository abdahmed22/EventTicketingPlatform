package EventTicketing.controller;

import EventTicketing.dto.seatCategoryDto;
import EventTicketing.model.User;
import EventTicketing.service.seatCategoryService;
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
public class seatCategoryController {

    private final seatCategoryService seatCategoryService;

    @GetMapping("/events/{eventId}/seat-categories")
    public ResponseEntity<List<seatCategoryDto.Summary>> listByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(seatCategoryService.listByEvent(eventId));
    }

    @PostMapping("/organizer/events/{eventId}/seat-categories")
    public ResponseEntity<seatCategoryDto.Response> create(@AuthenticationPrincipal User organizer,
            @PathVariable UUID eventId,
            @Valid @RequestBody seatCategoryDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatCategoryService.create(eventId, organizer, request));
    }

    @PutMapping("/organizer/seat-categories/{id}")
    public ResponseEntity<seatCategoryDto.Response> update(@AuthenticationPrincipal User organizer,
            @PathVariable UUID id,
            @Valid @RequestBody seatCategoryDto.UpdateRequest request) {
        return ResponseEntity.ok(seatCategoryService.update(id, organizer, request));
    }
}
