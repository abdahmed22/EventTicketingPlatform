package EventTicketing.dto;

import EventTicketing.model.Event;

import java.time.LocalDateTime;
import java.util.UUID;

public class eventDto {

    public record CreateRequest(
            String title,
            String description,
            Event.Category category,
            LocalDateTime dateTime,
            UUID venueId,
            UUID organizerId) {
    }

    public record UpdateRequest(
            String title,
            String description,
            Event.Category category,
            LocalDateTime dateTime,
            UUID venueId) {
    }

    public record Summary(
            UUID id,
            String title,
            String description,
            Event.Category category,
            LocalDateTime dateTime,
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
            LocalDateTime dateTime,
            Event.Status status,
            UUID venueId,
            UUID organizerId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
