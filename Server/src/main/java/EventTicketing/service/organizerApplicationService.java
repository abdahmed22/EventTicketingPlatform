package EventTicketing.service;

import EventTicketing.dto.authDto;
import EventTicketing.dto.organizerApplicationDto;
import EventTicketing.exception.DuplicateResourceException;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.OrganizerApplication;
import EventTicketing.model.User;
import EventTicketing.model.enums.OrganizerApplicationStatus;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.organizerApplicationRepository;
import EventTicketing.repository.userRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class organizerApplicationService {

    private final organizerApplicationRepository organizerApplicationRepository;
    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public organizerApplicationDto.Response submit(organizerApplicationDto.SubmitRequest request) {
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
                .passwordHash(passwordEncoder.encode(request.password()))
                .organizationName(request.organizationName())
                .reason(request.reason())
                .status(OrganizerApplicationStatus.PENDING)
                .build();

        return organizerApplicationDto.Response.from(organizerApplicationRepository.save(application));
    }

    public List<organizerApplicationDto.Response> list(OrganizerApplicationStatus status) {
        OrganizerApplicationStatus effectiveStatus = status != null ? status : OrganizerApplicationStatus.PENDING;
        return organizerApplicationRepository.findByStatus(effectiveStatus).stream()
                .map(organizerApplicationDto.Response::from)
                .toList();
    }

    @Transactional
    public authDto.UserSummary approve(UUID applicationId, User admin) {
        OrganizerApplication application = getPendingOrThrow(applicationId);

        User organizer = User.builder()
                .name(application.getName())
                .email(application.getEmail())
                .phone(application.getPhone())
                .password(application.getPasswordHash())
                .role(UserRole.ORGANIZER)
                .build();
        userRepository.save(organizer);

        application.setStatus(OrganizerApplicationStatus.APPROVED);
        application.setReviewedAt(Instant.now());
        application.setReviewedBy(admin);
        organizerApplicationRepository.save(application);

        return authDto.UserSummary.from(organizer);
    }

    @Transactional
    public organizerApplicationDto.Response reject(UUID applicationId, User admin, organizerApplicationDto.RejectRequest request) {
        OrganizerApplication application = getPendingOrThrow(applicationId);

        application.setStatus(OrganizerApplicationStatus.REJECTED);
        application.setReviewedAt(Instant.now());
        application.setReviewedBy(admin);
        application.setRejectionReason(request != null ? request.rejectionReason() : null);

        return organizerApplicationDto.Response.from(organizerApplicationRepository.save(application));
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