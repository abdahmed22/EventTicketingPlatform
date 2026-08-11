package EventTicketing.controller;


import EventTicketing.dto.VenueDto;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.service.VenueService;
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

public class VenueController {
    private final VenueService venueService;

    @GetMapping("/events/venues")
    public ResponseEntity<List<VenueDto.Response>> listPublicApprovedVenues() {
        return ResponseEntity.ok(venueService.listPendingOrAll(Venue.Status.APPROVED));
    }

    @PostMapping("/organizer/venues")
    public ResponseEntity<VenueDto.Response> submit(@AuthenticationPrincipal User organizer,
                                                    @Valid @RequestBody VenueDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.submit(organizer, request));
    }

    @PutMapping("/organizer/venues/{id}")
    public ResponseEntity<VenueDto.Response> updateOrganizerVenue(@AuthenticationPrincipal User organizer,
                                                            @PathVariable UUID id,
                                                            @Valid @RequestBody VenueDto.CreateRequest request) {
        return ResponseEntity.ok(venueService.update(id, request, organizer));
    }

    @GetMapping("/organizer/venues")
    public ResponseEntity<List<VenueDto.Response>> listMyVenues(@AuthenticationPrincipal User organizer,
                                                        @RequestParam(required = false) Venue.Status status) {
        return ResponseEntity.ok(venueService.listMyVenues(organizer, status));
    }

    @GetMapping("/admin/venues")
    public ResponseEntity<List<VenueDto.Response>> listAdminVenues(
            @RequestParam(required = false) Venue.Status status) {
        return ResponseEntity.ok(venueService.listPendingOrAll(status));
    }

    @PutMapping("/admin/venues/{id}")
    public ResponseEntity<VenueDto.Response> updateAdminVenue(@AuthenticationPrincipal User admin,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody VenueDto.CreateRequest request) {
        return ResponseEntity.ok(venueService.update(id, request, admin));
    }

    @PostMapping("/admin/venues/{id}/approve")
    public ResponseEntity<VenueDto.Response> approve(@AuthenticationPrincipal User admin,
                                                     @PathVariable UUID id) {
        return ResponseEntity.ok(venueService.approve(id, admin));
    }

    @PostMapping("/admin/venues/{id}/reject")
    public ResponseEntity<VenueDto.Response> reject(@AuthenticationPrincipal User admin,
                                                    @PathVariable UUID id,
                                                    @RequestBody(required = false) VenueDto.RejectRequest request) {
        return ResponseEntity.ok(venueService.reject(id, admin, request));
    }

    @DeleteMapping("/admin/venues/{id}")
    public ResponseEntity<Void> adminDelete(@PathVariable UUID id) {
        venueService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }

}