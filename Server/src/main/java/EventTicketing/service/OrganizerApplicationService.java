package EventTicketing.service;

import EventTicketing.dto.AuthDto;
import EventTicketing.dto.OrganizerApplicationDto;
import EventTicketing.exception.DuplicateResourceException;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.OrganizerApplication;
import EventTicketing.model.User;
import EventTicketing.model.enums.OrganizerApplicationStatus;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.OrganizerApplicationRepository;
import EventTicketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizerApplicationService {

    private final OrganizerApplicationRepository organizerApplicationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public OrganizerApplicationDto.Response submit(OrganizerApplicationDto.SubmitRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (organizerApplicationRepository.existsByEmailAndStatus(request.email(), OrganizerApplicationStatus.PENDING)) {
            throw new DuplicateResourceException("An application with this email is already pending review");
        }

        OrganizerApplication application = OrganizerApplication.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .organizationName(request.organizationName())
                .reason(request.reason())
                .status(OrganizerApplicationStatus.PENDING)
                .build();

        return OrganizerApplicationDto.Response.from(organizerApplicationRepository.save(application));
    }

    public List<OrganizerApplicationDto.Response> list(OrganizerApplicationStatus status) {
        List<OrganizerApplication> applications = status != null
                ? organizerApplicationRepository.findByStatus(status)
                : organizerApplicationRepository.findAll();

        return applications.stream()
                .map(OrganizerApplicationDto.Response::from)
                .toList();
    }

    @Transactional
    public AuthDto.UserSummary approve(UUID applicationId, User admin) {
        OrganizerApplication application = getPendingOrThrow(applicationId);

        User organizer = User.builder()
                .name(application.getName())
                .email(application.getEmail())
                .phone(application.getPhone())
                .password(application.getPassword())
                .role(UserRole.ORGANIZER)
                .build();
        userRepository.save(organizer);

        application.setStatus(OrganizerApplicationStatus.APPROVED);
        application.setReviewedAt(Instant.now());
        application.setReviewedBy(admin);
        organizerApplicationRepository.save(application);

        return AuthDto.UserSummary.from(organizer);
    }

    @Transactional
    public OrganizerApplicationDto.Response reject(UUID applicationId, User admin, OrganizerApplicationDto.RejectRequest request) {
        OrganizerApplication application = getPendingOrThrow(applicationId);

        application.setStatus(OrganizerApplicationStatus.REJECTED);
        application.setReviewedAt(Instant.now());
        application.setReviewedBy(admin);
        application.setRejectionReason(request != null ? request.rejectionReason() : null);

        return OrganizerApplicationDto.Response.from(organizerApplicationRepository.save(application));
    }

    private OrganizerApplication getPendingOrThrow(UUID applicationId) {
        OrganizerApplication application = organizerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer application not found: " + applicationId));

        if (application.getStatus() != OrganizerApplicationStatus.PENDING) {
            throw new ForbiddenActionException("This application has already been reviewed");
        }
        return application;
    }
}