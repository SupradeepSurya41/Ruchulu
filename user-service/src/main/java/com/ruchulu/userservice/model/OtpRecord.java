package com.ruchulu.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Audit record for every OTP send event.
 * Used for rate-limiting and security audit trail.
 */
@Entity
@Table(
    name = "otp_records",
    indexes = {
        @Index(name = "idx_otp_email",     columnList = "email"),
        @Index(name = "idx_otp_user_id",   columnList = "user_id"),
        @Index(name = "idx_otp_created_at",columnList = "created_at")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OtpRecord {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;           // BCrypt hash of the OTP (never plain)

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isUsable() {
        return !this.used && !isExpired();
    }
}
