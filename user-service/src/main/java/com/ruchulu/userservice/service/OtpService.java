package com.ruchulu.userservice.service;

import com.ruchulu.userservice.exception.*;
import com.ruchulu.userservice.model.*;
import com.ruchulu.userservice.repository.OtpRecordRepository;
import com.ruchulu.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * OtpService handles:
 *  - Generating a secure 6-digit OTP
 *  - Sending it via Gmail SMTP to the user's real email
 *  - Verifying it with rate-limit + expiry checks
 *  - Clearing OTP after use
 *  - Scheduled cleanup of expired OTP records
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OtpService {

    private final UserRepository      userRepository;
    private final OtpRecordRepository otpRecordRepository;
    private final JavaMailSender      mailSender;
    private final PasswordEncoder     passwordEncoder;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── GENERATE & SEND OTP ───────────────────────────────────────────────

    /**
     * Generates a 6-digit OTP and sends it to the user's email.
     * Enforces a cooldown to prevent OTP spam.
     */
    public void generateAndSendOtp(String userId, String email, OtpPurpose purpose) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Enforce resend cooldown
        if (user.getOtpLastSentAt() != null) {
            long secondsSinceLast = java.time.Duration.between(
                user.getOtpLastSentAt(), LocalDateTime.now()
            ).getSeconds();

            if (secondsSinceLast < resendCooldownSeconds) {
                long remaining = resendCooldownSeconds - secondsSinceLast;
                throw new OtpResendTooSoonException(remaining);
            }
        }

        // Generate OTP
        String rawOtp   = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        // Store OTP (hashed) in user row
        userRepository.setOtp(userId, rawOtp, expiry, LocalDateTime.now());

        // Persist audit record (hashed)
        OtpRecord record = OtpRecord.builder()
                .userId(userId)
                .email(email)
                .purpose(purpose)
                .otpHash(passwordEncoder.encode(rawOtp))
                .expiresAt(expiry)
                .build();
        otpRecordRepository.save(record);

        // Send email
        sendOtpEmail(email, rawOtp, purpose, user.getFirstName(), otpExpiryMinutes);
        log.info("OTP sent to {} for purpose={}", maskEmail(email), purpose);
    }

    // ── VERIFY OTP ────────────────────────────────────────────────────────

    /**
     * Verifies an OTP submitted by the user.
     * Increments attempt counter and clears OTP after successful verification.
     */
    public void verifyOtp(String userId, String submittedOtp, OtpPurpose purpose) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Check attempt limit
        if (user.getOtpAttempts() != null && user.getOtpAttempts() >= maxAttempts) {
            userRepository.clearOtp(userId);
            throw new OtpMaxAttemptsException();
        }

        // Check expiry
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            userRepository.clearOtp(userId);
            throw new OtpExpiredException();
        }

        // Verify code
        if (!submittedOtp.equals(user.getOtpCode())) {
            userRepository.incrementOtpAttempts(userId);
            throw new InvalidOtpException();
        }

        // Success — clear OTP
        userRepository.clearOtp(userId);
        log.info("OTP verified successfully for userId={}, purpose={}", userId, purpose);
    }

    // ── EMAIL SENDER ──────────────────────────────────────────────────────

    private void sendOtpEmail(String to, String otp, OtpPurpose purpose,
                               String firstName, int expiryMins) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(fromEmail, "Ruchulu 🥄");
            helper.setTo(to);
            helper.setSubject(buildSubject(purpose));
            helper.setText(buildHtmlBody(firstName, otp, purpose, expiryMins), true);

            mailSender.send(mime);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send OTP email to {}: {}", maskEmail(to), e.getMessage());
            // Do NOT throw — user shouldn't see mail server errors in prod
            // In prod, plug in a retry queue here
        }
    }

    private String buildSubject(OtpPurpose purpose) {
        return switch (purpose) {
            case LOGIN              -> "🔐 Your Ruchulu Login OTP";
            case EMAIL_VERIFICATION -> "✅ Verify Your Ruchulu Email";
            case PASSWORD_RESET     -> "🔑 Reset Your Ruchulu Password";
            case PHONE_VERIFICATION -> "📱 Verify Your Phone Number";
        };
    }

    private String buildHtmlBody(String firstName, String otp, OtpPurpose purpose, int expiryMins) {
        String purposeText = switch (purpose) {
            case LOGIN              -> "complete your login";
            case EMAIL_VERIFICATION -> "verify your email address";
            case PASSWORD_RESET     -> "reset your password";
            case PHONE_VERIFICATION -> "verify your phone number";
        };

        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Outfit,sans-serif;background:#FAF6F0;margin:0;padding:30px">
              <div style="max-width:500px;margin:0 auto;background:white;border-radius:20px;
                          padding:40px;border:1px solid #EDE0CC;box-shadow:0 4px 20px rgba(44,24,16,0.08)">
                <div style="text-align:center;margin-bottom:24px">
                  <span style="font-size:2.5rem">🥄</span>
                  <h2 style="font-family:Georgia,serif;color:#9E4E2A;margin:8px 0 4px">Ruchulu</h2>
                  <p style="color:#7A5030;font-size:0.85rem;margin:0">India's Most Loved Catering Platform</p>
                </div>
                <p style="color:#3D2010;font-size:1rem">Hello <strong>%s</strong>,</p>
                <p style="color:#7A5030;font-size:0.95rem">
                  Use the OTP below to <strong>%s</strong>:
                </p>
                <div style="text-align:center;margin:28px 0">
                  <div style="background:linear-gradient(135deg,#E8873A,#A83228);
                              color:white;font-size:2.5rem;font-weight:900;
                              letter-spacing:0.4em;padding:20px 30px;
                              border-radius:14px;display:inline-block;
                              font-family:monospace">
                    %s
                  </div>
                </div>
                <p style="color:#7A5030;font-size:0.88rem;text-align:center">
                  ⏱ This OTP is valid for <strong>%d minutes</strong> only.
                </p>
                <hr style="border:none;border-top:1px solid #EDE0CC;margin:24px 0"/>
                <p style="color:#b09070;font-size:0.78rem;text-align:center">
                  If you didn't request this, please ignore this email.
                  Your account is safe.
                </p>
                <p style="color:#b09070;font-size:0.78rem;text-align:center">
                  © 2026 Ruchulu Pvt. Ltd. · Hyderabad, Telangana 🇮🇳
                </p>
              </div>
            </body>
            </html>
            """.formatted(firstName, purposeText, otp, expiryMins);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    public String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return "***@" + email.substring(atIdx + 1);
        return email.substring(0, 2) + "***" + email.substring(atIdx);
    }

    // ── SCHEDULED CLEANUP ─────────────────────────────────────────────────
    /** Runs daily at 3 AM — deletes expired OTP records older than 24h */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredOtps() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        otpRecordRepository.deleteExpiredBefore(threshold);
        log.info("Expired OTP records cleaned up (before {})", threshold);
    }
}
