package EventTicketing.controller;

import EventTicketing.dto.venueDto;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.service.venueService;
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
public class venueController {

    private final venueService venueService;

    @PostMapping("/organizer/venues")
    public ResponseEntity<venueDto.Response> submit(@AuthenticationPrincipal User organizer,
            @Valid @RequestBody venueDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.submit(organizer, request));
    }

    @GetMapping("/organizer/venues")
    public ResponseEntity<List<venueDto.Response>> listMyVenues(@AuthenticationPrincipal User organizer) {
        return ResponseEntity.ok(venueService.listMyVenues(organizer));
    }

    @GetMapping("/admin/venues")
    public ResponseEntity<List<venueDto.Response>> listAdminVenues(
            @RequestParam(required = false) Venue.Status status) {
        return ResponseEntity.ok(venueService.listPendingOrAll(status));
    }

    @PostMapping("/admin/venues/{id}/approve")
    public ResponseEntity<venueDto.Response> approve(@AuthenticationPrincipal User admin,
            @PathVariable UUID id) {
        return ResponseEntity.ok(venueService.approve(id, admin));
    }

    @PostMapping("/admin/venues/{id}/reject")
    public ResponseEntity<venueDto.Response> reject(@AuthenticationPrincipal User admin,
            @PathVariable UUID id,
            @RequestBody(required = false) venueDto.RejectRequest request) {
        return ResponseEntity.ok(venueService.reject(id, admin, request));
    }
}
