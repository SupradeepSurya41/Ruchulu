package com.ruchulu.userservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Core User entity — maps to the 'users' table.
 * Covers both CUSTOMER and CATERER roles from the Ruchulu HTML.
 * Passwords are ALWAYS stored as BCrypt hashes — never plain text.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_email",  columnNames = "email"),
        @UniqueConstraint(name = "uk_user_phone",  columnNames = "phone")
    },
    indexes = {
        @Index(name = "idx_user_email",  columnList = "email"),
        @Index(name = "idx_user_role",   columnList = "role"),
        @Index(name = "idx_user_city",   columnList = "city"),
        @Index(name = "idx_user_status", columnList = "account_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"passwordHash", "emailVerificationToken", "passwordResetToken"})
@EqualsAndHashCode(of = "id")
public class User {

    // ─── Identity ────────────────────────────────────────────────────
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    // ─── Personal Info ────────────────────────────────────────────────
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2–50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'\\-]+$", message = "First name can only contain letters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2–50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'\\-]+$", message = "Last name can only contain letters")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone must be a valid 10-digit Indian number starting with 6, 7, 8, or 9"
    )
    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String phone;

    // ─── Auth ─────────────────────────────────────────────────────────
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    // ─── Location ─────────────────────────────────────────────────────
    @Column(name = "city", length = 60)
    private String city;

    // ─── Account State ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    // ─── Tokens ───────────────────────────────────────────────────────
    @Column(name = "email_verification_token", length = 100)
    private String emailVerificationToken;

    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    // ─── OTP fields ───────────────────────────────────────────────────
    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "otp_attempts", nullable = false)
    @Builder.Default
    private Integer otpAttempts = 0;

    @Column(name = "otp_last_sent_at")
    private LocalDateTime otpLastSentAt;

    // ─── Profile ──────────────────────────────────────────────────────
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ─── Timestamps ───────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Soft Delete ──────────────────────────────────────────────────
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ─── Domain helpers ───────────────────────────────────────────────
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.accountStatus) && !Boolean.TRUE.equals(this.deleted);
    }

    public void markDeleted() {
        this.deleted    = true;
        this.deletedAt  = LocalDateTime.now();
        this.accountStatus = AccountStatus.DELETED;
    }

    public void clearOtp() {
        this.otpCode     = null;
        this.otpExpiry   = null;
        this.otpAttempts = 0;
    }

    public boolean isOtpValid(String code) {
        return this.otpCode != null
            && this.otpCode.equals(code)
            && this.otpExpiry != null
            && LocalDateTime.now().isBefore(this.otpExpiry);
    }
}
