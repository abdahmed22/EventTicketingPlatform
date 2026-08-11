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

    // =========================================================
    // PUBLIC ENDPOINTS
    // =========================================================

    // Get all approved venues.
    // This endpoint is publicly accessible and is used when users
    // need to view venues that have already been approved by an admin.
    @GetMapping("/events/venues")
    public ResponseEntity<List<VenueDto.Response>> listPublicApprovedVenues() {
        return ResponseEntity.ok(
                venueService.listPendingOrAll(Venue.Status.APPROVED)
        );
    }


    // =========================================================
    // ORGANIZER ENDPOINTS
    // =========================================================

    // Create/submit a new venue.
    // The currently authenticated organizer is automatically obtained
    // using @AuthenticationPrincipal and passed to the service.
    @PostMapping("/organizer/venues")
    public ResponseEntity<VenueDto.Response> submit(
            @AuthenticationPrincipal User organizer,
            @Valid @RequestBody VenueDto.CreateRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(venueService.submit(organizer, request));
    }


    // Update an existing venue belonging to the organizer.
    // The venue ID is taken from the URL and the updated venue data
    // is received in the request body.
    @PutMapping("/organizer/venues/{id}")
    public ResponseEntity<VenueDto.Response> updateOrganizerVenue(
            @AuthenticationPrincipal User organizer,
            @PathVariable UUID id,
            @Valid @RequestBody VenueDto.CreateRequest request) {

        return ResponseEntity.ok(
                venueService.update(id, request, organizer)
        );
    }


    // Get all venues created by the currently authenticated organizer.
    // The optional "status" parameter can be used to filter the results.
    //
    // Example:
    // GET /api/organizer/venues?status=APPROVED
    @GetMapping("/organizer/venues")
    public ResponseEntity<List<VenueDto.Response>> listMyVenues(
            @AuthenticationPrincipal User organizer,
            @RequestParam(required = false) Venue.Status status) {

        return ResponseEntity.ok(
                venueService.listMyVenues(organizer, status)
        );
    }


    // =========================================================
    // ADMIN ENDPOINTS
    // =========================================================

    // Get venues for administration.
    // The optional status parameter allows the admin to filter venues.
    //
    // Example:
    // GET /api/admin/venues?status=PENDING
    @GetMapping("/admin/venues")
    public ResponseEntity<List<VenueDto.Response>> listAdminVenues(
            @RequestParam(required = false) Venue.Status status) {

        return ResponseEntity.ok(
                venueService.listPendingOrAll(status)
        );
    }


    // Update an existing venue as an administrator.
    // Unlike the organizer update endpoint, the authenticated user
    // is treated as an admin by the service layer.
    @PutMapping("/admin/venues/{id}")
    public ResponseEntity<VenueDto.Response> updateAdminVenue(
            @AuthenticationPrincipal User admin,
            @PathVariable UUID id,
            @Valid @RequestBody VenueDto.CreateRequest request) {

        return ResponseEntity.ok(
                venueService.update(id, request, admin)
        );
    }


    // Approve a venue submitted by an organizer.
    // The venue ID comes from the URL and the authenticated admin
    // performs the approval action.
    @PostMapping("/admin/venues/{id}/approve")
    public ResponseEntity<VenueDto.Response> approve(
            @AuthenticationPrincipal User admin,
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                venueService.approve(id, admin)
        );
    }


    // Reject a venue submitted by an organizer.
    // A rejection request body can optionally contain additional
    // information, such as the reason for rejection.
    @PostMapping("/admin/venues/{id}/reject")
    public ResponseEntity<VenueDto.Response> reject(
            @AuthenticationPrincipal User admin,
            @PathVariable UUID id,
            @RequestBody(required = false) VenueDto.RejectRequest request) {

        return ResponseEntity.ok(
                venueService.reject(id, admin, request)
        );
    }


    // Delete a venue as an administrator.
    // Returns HTTP 204 (No Content) when the deletion is successful.
    @DeleteMapping("/admin/venues/{id}")
    public ResponseEntity<Void> adminDelete(@PathVariable UUID id) {
        venueService.adminDelete(id);

        return ResponseEntity.noContent().build();
    }


    // Create a venue directly as an administrator.
    // Unlike an organizer submission, an admin-created venue may
    // follow different approval rules handled by the service layer.
    @PostMapping("/admin/venues")
    public ResponseEntity<VenueDto.Response> adminCreate(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody VenueDto.CreateRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(venueService.adminCreate(admin, request));
    }
}

