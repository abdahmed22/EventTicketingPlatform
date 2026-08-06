package EventTicketing.dto;

import EventTicketing.model.Event;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class eventDto {

        public record CreateRequest(
                        String title,
                        String description,
                        Event.Category category,
                        LocalDate eventDate,
                        LocalTime eventTime,
                        UUID venueId) {
        }

        public record UpdateRequest(
                        String title,
                        String description,
                        Event.Category category,
                        LocalDate eventDate,
                        LocalTime eventTime,
                        UUID venueId) {
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
                        UUID venueId,
                        UUID organizerId,
                        List<seatCategoryDto.Summary> seatCategories,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        }
}
