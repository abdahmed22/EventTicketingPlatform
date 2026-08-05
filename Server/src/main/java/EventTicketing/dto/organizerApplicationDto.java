package EventTicketing.dto;

import EventTicketing.model.OrganizerApplication;
import EventTicketing.model.enums.OrganizerApplicationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public abstract class organizerApplicationDto {

    public record SubmitRequest(
            @NotBlank(message = "Name is required")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid address")
            String email,

            @NotBlank(message = "Phone is required")
            String phone,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,

            String organizationName,

            String reason
    ) {}

    public record RejectRequest(
            String rejectionReason
    ) {}

    public record Response(
            UUID id,
            String name,
            String email,
            String phone,
            String organizationName,
            String reason,
            OrganizerApplicationStatus status,
            Instant submittedAt,
            Instant reviewedAt,
            String reviewedByName,
            String rejectionReason
    ) {
        public static Response from(OrganizerApplication application) {
            return new Response(
                    application.getId(),
                    application.getName(),
                    application.getEmail(),
                    application.getPhone(),
                    application.getOrganizationName(),
                    application.getReason(),
                    application.getStatus(),
                    application.getSubmittedAt(),
                    application.getReviewedAt(),
                    application.getReviewedBy() != null ? application.getReviewedBy().getName() : null,
                    application.getRejectionReason()
            );
        }
    }
}