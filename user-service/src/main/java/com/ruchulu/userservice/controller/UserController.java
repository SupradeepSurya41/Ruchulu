package com.ruchulu.userservice.controller;

import com.ruchulu.userservice.dto.*;
import com.ruchulu.userservice.model.User;
import com.ruchulu.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * UserController — profile management.
 * Base URL: /api/v1/users
 * All endpoints require a valid JWT (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    // ── GET OWN PROFILE ───────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", toProfileMap(user)
        ));
    }

    // ── GET ANY USER BY ID (admin use) ────────────────────────────────────
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", toProfileMap(user)
        ));
    }

    // ── UPDATE PROFILE ────────────────────────────────────────────────────
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {

        String userId = (String) auth.getPrincipal();
        User updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Profile updated successfully",
            "data", toProfileMap(updated)
        ));
    }

    // ── CHANGE PASSWORD ───────────────────────────────────────────────────
    @PostMapping("/me/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {

        String userId = (String) auth.getPrincipal();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Password changed successfully. Please log in again.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── DEACTIVATE ACCOUNT ────────────────────────────────────────────────
    @PostMapping("/me/deactivate")
    public ResponseEntity<Map<String, Object>> deactivate(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        userService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Your account has been deactivated. Contact support@ruchulu.com to reactivate.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── DELETE ACCOUNT ────────────────────────────────────────────────────
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> deleteAccount(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        userService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Your account has been permanently deleted. We're sorry to see you go.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── HELPER ────────────────────────────────────────────────────────────
    private Map<String, Object> toProfileMap(User u) {
        return Map.ofEntries(
            Map.entry("id",            u.getId()),
            Map.entry("firstName",     u.getFirstName()),
            Map.entry("lastName",      u.getLastName()),
            Map.entry("fullName",      u.getFullName()),
            Map.entry("email",         u.getEmail()),
            Map.entry("phone",         u.getPhone()),
            Map.entry("city",          u.getCity() != null ? u.getCity() : ""),
            Map.entry("role",          u.getRole().name()),
            Map.entry("accountStatus", u.getAccountStatus().name()),
            Map.entry("emailVerified", u.getEmailVerified()),
            Map.entry("phoneVerified", u.getPhoneVerified()),
            Map.entry("createdAt",     u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
        );
    }
}
