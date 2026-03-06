package com.ruchulu.bookingservice.controller;

import com.ruchulu.bookingservice.dto.*;
import com.ruchulu.bookingservice.model.*;
import com.ruchulu.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * BookingController — all customer and caterer booking endpoints.
 * Base URL: /api/v1/bookings
 *
 * Frontend → API mapping:
 *   "Book Now"          → POST /bookings
 *   "My Bookings"       → GET  /bookings/my
 *   "View Booking"      → GET  /bookings/{id}
 *   "Cancel Booking"    → POST /bookings/{id}/cancel
 *   "Pay Advance"       → POST /bookings/{id}/pay
 *   "View Timeline"     → GET  /bookings/{id}/timeline
 *
 * Caterer frontend:
 *   "My Orders"         → GET  /bookings/caterer/incoming
 *   "Accept"            → POST /bookings/{id}/confirm
 *   "Decline"           → POST /bookings/{id}/reject
 *   "Mark Complete"     → POST /bookings/{id}/complete
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    // ── PING ──────────────────────────────────────────────────────────────
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("service", "booking-service", "status", "UP",
            "timestamp", LocalDateTime.now().toString()));
    }

    // ═══════════════ CUSTOMER ENDPOINTS ══════════════════════════════════

    // ── CREATE BOOKING ────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<Booking> createBooking(
            Authentication auth,
            @Valid @RequestBody CreateBookingRequest request) {

        String userId = userId(auth);
        // In real flow, customer name/email/phone fetched from user-service via JWT claims or feign
        String name  = claimOrDefault(auth, "firstName", "Customer");
        String email = claimOrDefault(auth, "email", "");
        String phone = "";

        Booking booking = bookingService.createBooking(userId, name, email, phone, request);
        log.info("POST /bookings created: id={}", booking.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    // ── GET MY BOOKINGS ───────────────────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyBookings(
            Authentication auth,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Booking> bookings = bookingService.getMyBookings(userId(auth), status, page, size);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", bookings.getContent(),
            "totalElements", bookings.getTotalElements(),
            "totalPages", bookings.getTotalPages(),
            "currentPage", bookings.getNumber()
        ));
    }

    // ── GET BOOKING BY ID ─────────────────────────────────────────────────
    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBooking(
            @PathVariable String bookingId,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, userId(auth)));
    }

    // ── CANCEL BOOKING ────────────────────────────────────────────────────
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable String bookingId,
            Authentication auth,
            @Valid @RequestBody CancelBookingRequest request) {

        bookingService.cancelBooking(bookingId, userId(auth), request.getReason());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Booking cancelled successfully.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── RECORD PAYMENT ────────────────────────────────────────────────────
    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<Map<String, Object>> recordPayment(
            @PathVariable String bookingId,
            Authentication auth,
            @Valid @RequestBody PaymentRequest request) {

        request.setBookingId(bookingId);
        bookingService.recordPayment(bookingId, userId(auth), request);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Payment recorded successfully.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── GET TIMELINE ──────────────────────────────────────────────────────
    @GetMapping("/{bookingId}/timeline")
    public ResponseEntity<Map<String, Object>> getTimeline(
            @PathVariable String bookingId,
            Authentication auth) {

        List<BookingEvent> events = bookingService.getTimeline(bookingId, userId(auth));
        return ResponseEntity.ok(Map.of("success", true, "data", events));
    }

    // ═══════════════ CATERER ENDPOINTS ═══════════════════════════════════

    // ── GET INCOMING BOOKINGS (caterer) ───────────────────────────────────
    @GetMapping("/caterer/incoming")
    public ResponseEntity<Map<String, Object>> getCatererBookings(
            Authentication auth,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Booking> bookings = bookingService.getCatererBookings(userId(auth), status, page, size);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", bookings.getContent(),
            "totalElements", bookings.getTotalElements(),
            "totalPages", bookings.getTotalPages()
        ));
    }

    // ── CONFIRM BOOKING (caterer) ─────────────────────────────────────────
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmBooking(
            @PathVariable String bookingId,
            Authentication auth,
            @RequestBody(required = false) ConfirmBookingRequest request) {

        String notes = request != null ? request.getCatererNotes() : null;
        bookingService.confirmBooking(bookingId, userId(auth), notes);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Booking confirmed successfully.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── REJECT BOOKING (caterer) ──────────────────────────────────────────
    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<Map<String, Object>> rejectBooking(
            @PathVariable String bookingId,
            Authentication auth,
            @Valid @RequestBody RejectBookingRequest request) {

        bookingService.rejectBooking(bookingId, userId(auth), request.getReason());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Booking rejected.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── MARK IN-PROGRESS (caterer) ────────────────────────────────────────
    @PostMapping("/{bookingId}/start")
    public ResponseEntity<Map<String, Object>> markInProgress(
            @PathVariable String bookingId,
            Authentication auth) {

        bookingService.markInProgress(bookingId, userId(auth));
        return ResponseEntity.ok(Map.of("success", true, "message", "Booking marked as in progress."));
    }

    // ── MARK COMPLETED (caterer) ──────────────────────────────────────────
    @PostMapping("/{bookingId}/complete")
    public ResponseEntity<Map<String, Object>> markCompleted(
            @PathVariable String bookingId,
            Authentication auth) {

        bookingService.markCompleted(bookingId, userId(auth));
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Booking marked complete. Customer can now leave a review.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private String userId(Authentication auth) {
        return (String) auth.getPrincipal();
    }

    private String claimOrDefault(Authentication auth, String claim, String defaultVal) {
        // In real JWT flow, extract from JWT claims
        return defaultVal;
    }
}
