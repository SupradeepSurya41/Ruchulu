package com.ruchulu.userservice.service;

import com.ruchulu.userservice.config.JwtService;
import com.ruchulu.userservice.dto.*;
import com.ruchulu.userservice.exception.*;
import com.ruchulu.userservice.model.*;
import com.ruchulu.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core business logic for all user operations.
 *
 * Two-step login flow:
 *   Step 1 — loginStep1(): validate credentials → send OTP
 *   Step 2 — loginStep2OtpVerify(): verify OTP → return JWT tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    // ── REGISTER ────────────────────────────────────────────────────────
    @Override
    public AuthResponse register(RegisterRequest req) {
        log.info("Register attempt: email={}", req.getEmail());

        // Duplicate checks
        if (userRepository.existsByEmailAndDeletedFalse(req.getEmail().toLowerCase())) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }
        if (userRepository.existsByPhoneAndDeletedFalse(req.getPhone())) {
            throw new PhoneAlreadyExistsException(req.getPhone());
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .email(req.getEmail().toLowerCase().trim())
                .phone(req.getPhone().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole() != null ? req.getRole() : UserRole.CUSTOMER)
                .city(req.getCity())
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .authProvider(AuthProvider.LOCAL)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={}, email={}", saved.getId(), saved.getEmail());

        // Send OTP for email verification
        try {
            otpService.generateAndSendOtp(saved.getId(), saved.getEmail(), OtpPurpose.EMAIL_VERIFICATION);
        } catch (Exception e) {
            log.warn("Could not send verification OTP: {}", e.getMessage());
        }

        String access  = jwtService.generateAccessToken(saved);
        String refresh = jwtService.generateRefreshToken(saved);

        return buildAuthResponse(saved, access, refresh,
            "Account created! A verification OTP has been sent to " + saved.getEmail() + ".", false);
    }

    // ── LOGIN STEP 1 — validate credentials, send OTP ───────────────────
    @Override
    public AuthResponse loginStep1(LoginRequest req) {
        log.info("Login step-1 attempt: identifier={}", req.getIdentifier());

        User user = userRepository.findByEmailOrPhone(req.getIdentifier())
                .orElseThrow(InvalidCredentialsException::new);

        guardAccountState(user);

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("Wrong password for: {}", user.getEmail());
            throw new InvalidCredentialsException();
        }

        // Send OTP
        otpService.generateAndSendOtp(user.getId(), user.getEmail(), OtpPurpose.LOGIN);

        // Return partial response — no JWT yet
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .otpRequired(true)
                .message("OTP sent to " + maskEmail(user.getEmail()) + ". Please enter it to complete login.")
                .build();
    }

    // ── LOGIN STEP 2 — verify OTP, return full JWT ───────────────────────
    @Override
    public AuthResponse loginStep2OtpVerify(OtpVerifyRequest req) {
        log.info("Login step-2 OTP verify: email={}", req.getEmail());

        User user = userRepository.findByEmailAndDeletedFalse(req.getEmail().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(req.getEmail()));

        guardAccountState(user);

        OtpPurpose purpose = OtpPurpose.valueOf(req.getPurpose().toUpperCase());
        otpService.verifyOtp(user.getId(), req.getOtp(), purpose);

        // For email_verification — mark account active
        if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
            user.setEmailVerified(true);
            user.setAccountStatus(AccountStatus.ACTIVE);
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String access  = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        log.info("Login complete: userId={}", user.getId());
        return buildAuthResponse(user, access, refresh,
            "Welcome back, " + user.getFirstName() + "! 🙏", false);
    }

    // ── EMAIL VERIFICATION (OTP path) ────────────────────────────────────
    @Override
    public void sendEmailVerificationOtp(String userId) {
        User user = getUserById(userId);
        otpService.generateAndSendOtp(userId, user.getEmail(), OtpPurpose.EMAIL_VERIFICATION);
    }

    @Override
    public void verifyEmailOtp(String userId, String otp) {
        User user = getUserById(userId);
        otpService.verifyOtp(userId, otp, OtpPurpose.EMAIL_VERIFICATION);
        userRepository.verifyEmail(userId);
        log.info("Email verified via OTP: userId={}", userId);
    }

    // ── EMAIL VERIFICATION (token / link path) ───────────────────────────
    @Override
    public void verifyEmailToken(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("email verification"));
        userRepository.verifyEmail(user.getId());
        log.info("Email verified via link: {}", user.getEmail());
    }

    // ── GET USER ─────────────────────────────────────────────────────────
    @Override
    public User getUserById(String id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    // ── UPDATE PROFILE ────────────────────────────────────────────────────
    @Override
    public User updateProfile(String userId, UpdateProfileRequest req) {
        User user = getUserById(userId);

        if (req.getPhone() != null && !req.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhoneAndDeletedFalse(req.getPhone())) {
                throw new PhoneAlreadyExistsException(req.getPhone());
            }
            user.setPhone(req.getPhone());
        }
        if (req.getFirstName()         != null) user.setFirstName(req.getFirstName().trim());
        if (req.getLastName()          != null) user.setLastName(req.getLastName().trim());
        if (req.getCity()              != null) user.setCity(req.getCity());
        if (req.getProfilePictureUrl() != null) user.setProfilePictureUrl(req.getProfilePictureUrl());

        return userRepository.save(user);
    }

    // ── CHANGE PASSWORD ───────────────────────────────────────────────────
    @Override
    public void changePassword(String userId, ChangePasswordRequest req) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new WrongCurrentPasswordException();
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed: userId={}", userId);
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────
    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(email));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Also send OTP as alternative reset method
        try {
            otpService.generateAndSendOtp(user.getId(), user.getEmail(), OtpPurpose.PASSWORD_RESET);
        } catch (Exception e) {
            log.warn("OTP send failed during forgot-password: {}", e.getMessage());
        }

        log.info("Password reset initiated for: {}", maskEmail(email));
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────────
    @Override
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordMismatchException();
        }

        User user = userRepository.findByValidPasswordResetToken(token, LocalDateTime.now())
                .orElseThrow(() -> new InvalidTokenException("password reset"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset completed: userId={}", user.getId());
    }

    // ── ACCOUNT LIFECYCLE ─────────────────────────────────────────────────
    @Override
    public void deactivateAccount(String userId) {
        User user = getUserById(userId);
        user.setAccountStatus(AccountStatus.DEACTIVATED);
        userRepository.save(user);
        log.info("Account deactivated: userId={}", userId);
    }

    @Override
    public void deleteAccount(String userId) {
        User user = getUserById(userId);
        user.markDeleted();
        userRepository.save(user);
        log.info("Account soft-deleted: userId={}", userId);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private void guardAccountState(User user) {
        if (Boolean.TRUE.equals(user.getDeleted()))                      throw new AccountDeletedException();
        if (AccountStatus.SUSPENDED.equals(user.getAccountStatus()))     throw new AccountSuspendedException("Contact support");
        if (AccountStatus.DEACTIVATED.equals(user.getAccountStatus()))   throw new AccountDeactivatedException();
    }

    private AuthResponse buildAuthResponse(User u, String access, String refresh, String msg, boolean otpRequired) {
        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(86400L)
                .userId(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .city(u.getCity())
                .role(u.getRole())
                .emailVerified(u.getEmailVerified())
                .profilePictureUrl(u.getProfilePictureUrl())
                .message(msg)
                .otpRequired(otpRequired)
                .build();
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return "***" + email.substring(at);
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
