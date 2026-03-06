package com.ruchulu.notificationservice.controller;

import com.ruchulu.notificationservice.dto.*;
import com.ruchulu.notificationservice.model.*;
import com.ruchulu.notificationservice.service.NotificationServiceImpl;
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
 * NotificationController — REST endpoints for notification management.
 * Base: /api/v1/notifications
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationServiceImpl notificationService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("service", "notification-service", "status", "UP",
            "timestamp", LocalDateTime.now().toString()));
    }

    /** Internal endpoint — called by other microservices */
    @PostMapping("/send")
    public ResponseEntity<Notification> send(@Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.send(request));
    }

    /** Send OTP email — called by user-service */
    @PostMapping("/send/otp")
    public ResponseEntity<Notification> sendOtp(@Valid @RequestBody OtpNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendOtp(request));
    }

    /** Fire booking notifications — called by booking-service */
    @PostMapping("/send/booking")
    public ResponseEntity<Map<String, Object>> sendBooking(
            @RequestBody BookingNotificationRequest request) {
        notificationService.sendBookingNotifications(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Booking notifications dispatched."));
    }

    /** Get current user's notifications */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = (String) auth.getPrincipal();
        Page<Notification> results = notificationService.getMyNotifications(userId, page, size);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", results.getContent(),
            "totalElements", results.getTotalElements(),
            "totalPages", results.getTotalPages()
        ));
    }

    /** Get current user's notification preferences */
    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreference>> getPreferences(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(notificationService.getPreferences(userId));
    }

    /** Update a preference (opt-in/out) */
    @PutMapping("/preferences")
    public ResponseEntity<Map<String, Object>> updatePreference(
            Authentication auth,
            @Valid @RequestBody PreferenceUpdateRequest request) {
        String userId = (String) auth.getPrincipal();
        notificationService.updatePreference(userId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Preference updated."));
    }
}
