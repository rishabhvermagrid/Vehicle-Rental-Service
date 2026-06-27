package com.rental.booking.controller;

import com.rental.booking.dto.request.CreateBookingRequest;
import com.rental.booking.dto.response.ApiResponse;
import com.rental.booking.dto.response.BookingResponse;
import com.rental.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal String userId) {

        String role = extractRole();
        BookingResponse booking = bookingService.createBooking(request, Long.parseLong(userId), role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully.", booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId) {

        String role = extractRole();
        BookingResponse booking = bookingService.getBookingById(id, Long.parseLong(userId), role);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully.", booking));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal String userId) {

        List<BookingResponse> bookings = bookingService.getMyBookings(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully.", bookings));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        List<BookingResponse> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(ApiResponse.success("All bookings retrieved successfully.", bookings));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId) {

        String role = extractRole();
        BookingResponse booking = bookingService.cancelBooking(id, Long.parseLong(userId), role);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully.", booking));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(@PathVariable Long id) {
        BookingResponse booking = bookingService.completeBooking(id);
        return ResponseEntity.ok(ApiResponse.success("Booking completed successfully.", booking));
    }

    private String extractRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .map(a -> ((SimpleGrantedAuthority) a).getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("CUSTOMER");
    }
}
