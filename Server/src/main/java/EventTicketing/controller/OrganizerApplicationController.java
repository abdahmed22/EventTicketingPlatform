package EventTicketing.controller;

import EventTicketing.dto.AuthDto;
import EventTicketing.dto.OrganizerApplicationDto;
import EventTicketing.model.User;
import EventTicketing.model.enums.OrganizerApplicationStatus;
import EventTicketing.service.OrganizerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrganizerApplicationController {

    private final OrganizerApplicationService organizerApplicationService;

    @PostMapping("/api/register/organizer-application")
    public ResponseEntity<OrganizerApplicationDto.Response> submit(
            @Valid @RequestBody OrganizerApplicationDto.SubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizerApplicationService.submit(request));
    }

    @GetMapping("/api/admin/organizer-applications")
    public ResponseEntity<List<OrganizerApplicationDto.Response>> list(
            @RequestParam(required = false) OrganizerApplicationStatus status) {
        return ResponseEntity.ok(organizerApplicationService.list(status));
    }

    @PostMapping("/api/admin/organizer-applications/{id}/approve")
    public ResponseEntity<AuthDto.UserSummary> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(organizerApplicationService.approve(id, admin));
    }

    @PostMapping("/api/admin/organizer-applications/{id}/reject")
    public ResponseEntity<OrganizerApplicationDto.Response> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin,
            @RequestBody(required = false) OrganizerApplicationDto.RejectRequest request) {
        return ResponseEntity.ok(organizerApplicationService.reject(id, admin, request));
    }
}