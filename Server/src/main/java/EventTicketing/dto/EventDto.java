package EventTicketing.dto;

import EventTicketing.model.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class EventDto {
    public record CreateRequest(
            @NotBlank(message = "Title is required") @Size(max = 150, message = "Title must be at most 150 characters") String title,
            String description,
            @NotNull(message = "Category is required") Event.Category category,
            @NotNull(message = "Event date is required") LocalDate eventDate,
            @NotNull(message = "Event time is required") LocalTime eventTime,
            @NotNull(message = "Venue is required") UUID venueId) {
    }

    public record UpdateRequest(
            @Size(max = 150, message = "Title must be at most 150 characters") String title,
            String description,
            Event.Category category,
            LocalDate eventDate,
            LocalTime eventTime,
            UUID venueId) {
    }

    public record AdminUpdateRequest(
            @Size(max = 150, message = "Title must be at most 150 characters") String title,
            String description,
            Event.Category category,
            LocalDate eventDate,
            LocalTime eventTime,
            UUID venueId,
            Event.Status status) {
    }

    public record Summary(
            UUID id,
            String title,
            String description,
            Event.Category category,
            LocalDate eventDate,
            LocalTime eventTime,
            Event.Status status,
            UUID venueId,
            UUID organizerId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record Response(
            UUID id,
            String title,
            String description,
            Event.Category category,
            LocalDate eventDate,
            LocalTime eventTime,
            Event.Status status,
            VenueDto.Summary venue,
            UUID organizerId,
            List<SeatCategoryDto.Summary> seatCategories,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}