package EventTicketing.dto;

import java.time.Instant;
import java.util.List;

public record errorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static errorResponse of(int status, String error, String message) {
        return new errorResponse(Instant.now(), status, error, message, List.of());
    }

    public static errorResponse of(int status, String error, String message, List<FieldError> fieldErrors) {
        return new errorResponse(Instant.now(), status, error, message, fieldErrors);
    }
}