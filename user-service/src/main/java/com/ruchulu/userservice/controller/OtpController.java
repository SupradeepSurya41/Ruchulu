package com.ruchulu.userservice.controller;

import com.ruchulu.userservice.dto.OtpResendRequest;
import com.ruchulu.userservice.dto.OtpVerifyRequest;
import com.ruchulu.userservice.model.OtpPurpose;
import com.ruchulu.userservice.service.OtpService;
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
 * OtpController — handles OTP resend and email verification via OTP.
 * Base URL: /api/v1/otp
 */
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OtpController {

    private final OtpService  otpService;
    private final UserService userService;

    // ── RESEND OTP ────────────────────────────────────────────────────────
    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resendOtp(@Valid @RequestBody OtpResendRequest request) {
        log.info("POST /otp/resend — email: {}", request.getEmail());

        var user = userService.getUserByEmail(request.getEmail());
        OtpPurpose purpose = OtpPurpose.valueOf(request.getPurpose().toUpperCase());

        otpService.generateAndSendOtp(user.getId(), user.getEmail(), purpose);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "A new OTP has been sent to your email. Valid for 10 minutes.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── VERIFY EMAIL VIA OTP (logged-in user) ─────────────────────────────
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmailOtp(
            Authentication auth,
            @Valid @RequestBody OtpVerifyRequest request) {

        String userId = (String) auth.getPrincipal();
        userService.verifyEmailOtp(userId, request.getOtp());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Email verified successfully! Your account is now fully active.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
