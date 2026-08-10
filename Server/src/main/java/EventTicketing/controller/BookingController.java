package EventTicketing.controller;

import EventTicketing.dto.BookingDto;
import EventTicketing.model.User;
import EventTicketing.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;


    @PostMapping("/bookings")
    public ResponseEntity<BookingDto.Response> reserve(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookingDto.CreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingService.reserve(user, request));
    }


    @PostMapping("/bookings/{id}/confirm")
    public ResponseEntity<BookingDto.Response> confirm(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                bookingService.confirm(id, user)
        );
    }


    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingDto.Response> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                bookingService.cancel(id, user)
        );
    }


    @GetMapping("/bookings/my")
    public ResponseEntity<List<BookingDto.Response>> myBookings(
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                bookingService.myBookings(user)
        );
    }

    @GetMapping("/organizer/events/{eventId}/bookings")
    public ResponseEntity<List<BookingDto.Response>> getEventBookings(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal User organizer
    ) {
        return ResponseEntity.ok(
                bookingService.getEventBookings(eventId, organizer)
        );
    }

    @GetMapping("/admin/bookings")
    public ResponseEntity<List<BookingDto.Response>> getAllBookingsForAdmin(
            @AuthenticationPrincipal User admin
    ) {
        return ResponseEntity.ok(
                bookingService.getAllBookingsForAdmin(admin)
        );
    }
}