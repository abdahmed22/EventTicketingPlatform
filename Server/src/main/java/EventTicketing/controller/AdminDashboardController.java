package EventTicketing.controller;

import EventTicketing.dto.DashboardDTO;
import EventTicketing.dto.OrganizerApplicationDto;
import EventTicketing.dto.VenueDto;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.OrganizerApplicationStatus;
import EventTicketing.service.OrganizerApplicationService;
import EventTicketing.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrganizerApplicationService organizerApplicationService;
    private final VenueService venueService;
    
    @GetMapping
    public ResponseEntity<DashboardDTO.Summary> getDashboard() {
        List<OrganizerApplicationDto.Response> applications =
                organizerApplicationService.list(OrganizerApplicationStatus.PENDING);
        List<DashboardDTO.PendingVenue> venues = venueService.getPendingApplicationsForDashboard();

        List<DashboardDTO.PendingOrganizerApplication> queueOne = applications.stream()
                .map(app -> new DashboardDTO.PendingOrganizerApplication(
                        app.id(),
                        app.name(),
                        app.email(),
                        app.phone(),
                        app.organizationName(),
                        app.reason(),
                        app.submittedAt()))
                .toList();

        return ResponseEntity.ok(new DashboardDTO.Summary(
                queueOne.size(),
                queueOne,
                venues.size(),
                venues));
    }

    @GetMapping("/organizer-applications")
    public ResponseEntity<List<OrganizerApplicationDto.Response>> listOrganizerApplications(
            @RequestParam(required = false) OrganizerApplicationStatus status) {
        return ResponseEntity.ok(organizerApplicationService.list(status));
    }

    @PostMapping("/organizer-applications/{id}/approve")
    public ResponseEntity<?> approveOrganizerApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(organizerApplicationService.approve(id, admin));
    }

    @PostMapping("/organizer-applications/{id}/reject")
    public ResponseEntity<OrganizerApplicationDto.Response> rejectOrganizerApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin,
            @RequestBody(required = false) OrganizerApplicationDto.RejectRequest request) {
        return ResponseEntity.ok(organizerApplicationService.reject(id, admin, request));
    }

    @GetMapping("/venues")
    public ResponseEntity<List<VenueDto.Response>> listVenueRequests(
            @RequestParam(required = false) Venue.Status status) {
        return ResponseEntity.ok(venueService.listPendingOrAll(status));
    }

    @PostMapping("/venues/{id}/approve")
    public ResponseEntity<VenueDto.Response> approveVenue(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(venueService.approve(id, admin));
    }

    @PostMapping("/venues/{id}/reject")
    public ResponseEntity<VenueDto.Response> rejectVenue(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(venueService.rejectFromDashboard(id, admin));
    }
}
