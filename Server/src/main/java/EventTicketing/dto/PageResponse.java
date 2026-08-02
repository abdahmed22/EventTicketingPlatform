package EventTicketing.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}



//It's the standard shape for any endpoint that returns a paginated list, so every "list" response
// in the API looks the same regardless of what it's a list of. The SRS's browse-events endpoint
// (§6.2, FR-2.1) explicitly requires pagination — this is what that response is.
//
//Instead of EventController inventing its own pagination fields and BookingController
// inventing different ones, they both return PageResponse<EventDto.Summary> and
// PageResponse<BookingDto.Response> — same envelope, different content type via the generic.



//mmkn ne8ayar dh b3deen bas 8aleban dh feh requirements el project asln