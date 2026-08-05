package EventTicketing.controller;

import EventTicketing.dto.bookingDto;
import EventTicketing.model.User;
import EventTicketing.service.bookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class bookingController {

    private final bookingService bookingService;

    @PostMapping("/api/bookings/reserve")
    public ResponseEntity<bookingDto.Response> reserve(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody bookingDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.reserve(user, request));
    }

    @PostMapping("/api/bookings/{id}/confirm")
    public ResponseEntity<bookingDto.Response> confirm(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(bookingService.confirm(id, user));
    }

    @PostMapping("/api/bookings/{id}/cancel")
    public ResponseEntity<bookingDto.Response> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(bookingService.cancel(id, user));
    }

    @GetMapping("/api/bookings/my")
    public ResponseEntity<List<bookingDto.Response>> myBookings(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(bookingService.myBookings(user));
    }
}