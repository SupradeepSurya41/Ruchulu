package com.ruchulu.userservice.controller;

import com.ruchulu.userservice.dto.*;
import com.ruchulu.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AuthController — handles all authentication endpoints.
 * Base URL: /api/v1/auth
 *
 * Frontend button → endpoint mapping:
 *   "Create Account 🎉"  → POST /auth/register
 *   "Sign In →"          → POST /auth/login        (Step 1 — sends OTP)
 *   OTP submit           → POST /auth/verify-otp   (Step 2 — returns JWT)
 *   Forgot Password      → POST /auth/forgot-password
 *   Reset Password       → POST /auth/reset-password
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    // ── REGISTER ─────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register — email: {}", request.getEmail());
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── LOGIN STEP 1: credentials → OTP sent ─────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login — identifier: {}", request.getIdentifier());
        AuthResponse response = userService.loginStep1(request);
        return ResponseEntity.ok(response);
    }

    // ── LOGIN STEP 2: OTP → JWT tokens ───────────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        log.info("POST /auth/verify-otp — email: {}", request.getEmail());
        AuthResponse response = userService.loginStep2OtpVerify(request);
        return ResponseEntity.ok(response);
    }

    // ── EMAIL VERIFICATION via link ───────────────────────────────────────
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        userService.verifyEmailToken(token);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Email verified successfully! You can now log in.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "If that email is registered, you'll receive an OTP and reset link within 2 minutes.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(
            request.getToken(),
            request.getNewPassword(),
            request.getConfirmPassword()
        );
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Password reset successfully. Please log in with your new password.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── HEALTH PING ───────────────────────────────────────────────────────
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
            "service",   "user-service",
            "status",    "UP",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
