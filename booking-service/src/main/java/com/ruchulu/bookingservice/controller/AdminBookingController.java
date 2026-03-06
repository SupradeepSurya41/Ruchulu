package com.ruchulu.bookingservice.controller;

import com.ruchulu.bookingservice.dto.BookingFilterRequest;
import com.ruchulu.bookingservice.model.*;
import com.ruchulu.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AdminBookingController — admin-only booking management.
 * Base: /api/v1/admin/bookings
 */
@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) OccasionType occasion,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        BookingFilterRequest filter = new BookingFilterRequest();
        filter.setStatus(status);
        filter.setOccasion(occasion);
        filter.setCity(city);
        filter.setPage(page);
        filter.setSize(size);

        Page<Booking> results = bookingService.getAllBookings(filter);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", results.getContent(),
            "totalElements", results.getTotalElements(),
            "totalPages", results.getTotalPages()
        ));
    }

    @PostMapping("/{bookingId}/force-cancel")
    public ResponseEntity<Map<String, Object>> forceCancel(
            @PathVariable String bookingId,
            Authentication auth,
            @RequestParam String reason) {
        String adminId = (String) auth.getPrincipal();
        bookingService.forceCancel(bookingId, adminId, reason);
        return ResponseEntity.ok(Map.of("success", true,
            "message", "Booking force-cancelled by admin."));
    }
}
