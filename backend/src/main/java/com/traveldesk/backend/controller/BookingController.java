package com.traveldesk.backend.controller;

import com.traveldesk.backend.dto.booking.BookingRequest;
import com.traveldesk.backend.dto.booking.BookingResponse;
import com.traveldesk.backend.dto.booking.BookingStatusRequest;
import com.traveldesk.backend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'TRAVEL_AGENT')")
    public ResponseEntity<List<BookingResponse>> listBookings(
            @AuthenticationPrincipal UserDetails currentUser) {
        List<BookingResponse> bookings = isBookingManager(currentUser)
                ? bookingService.listAllBookings()
                : bookingService.listCustomerBookings(currentUser.getUsername());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'TRAVEL_AGENT')")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(bookingService.getBooking(
                id,
                currentUser.getUsername(),
                isBookingManager(currentUser)
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(
                currentUser.getUsername(),
                request
        );
        return ResponseEntity
                .created(URI.create("/api/bookings/" + response.id()))
                .body(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(
                bookingService.cancelCustomerBooking(id, currentUser.getUsername())
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAVEL_AGENT')")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusRequest request) {
        return ResponseEntity.ok(
                bookingService.updateBookingStatus(id, request.status())
        );
    }

    private boolean isBookingManager(UserDetails user) {
        return user.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_ADMIN"))
                || user.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_TRAVEL_AGENT"));
    }
}
