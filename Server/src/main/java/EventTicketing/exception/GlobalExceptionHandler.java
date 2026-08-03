package EventTicketing.exception;

import EventTicketing.dto.errorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<errorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<errorResponse> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<errorResponse> handleForbiddenAction(ForbiddenActionException ex) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN_ACTION", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<errorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<errorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new errorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                errorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                        "One or more fields are invalid", fieldErrors)
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<errorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email/phone or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<errorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<errorResponse> handleJwtException(JwtException ex) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired token");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<errorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error occurred");
    }

    private ResponseEntity<errorResponse> build(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status).body(errorResponse.of(status.value(), errorCode, message));
    }
}