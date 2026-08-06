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
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;


    @PostMapping
    public ResponseEntity<BookingDto.Response> reserve(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookingDto.CreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingService.reserve(user, request));
    }


    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingDto.Response> confirm(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                bookingService.confirm(id, user)
        );
    }


    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingDto.Response> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                bookingService.cancel(id, user)
        );
    }


    @GetMapping("/my")
    public ResponseEntity<List<BookingDto.Response>> myBookings(
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                bookingService.myBookings(user)
        );
    }
}