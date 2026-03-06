package com.ruchulu.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Notification — persisted record of every notification sent (or attempted).
 * Provides full audit trail: who, what channel, what status, when, retries.
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notif_type",      columnList = "notification_type"),
        @Index(name = "idx_notif_status",    columnList = "status"),
        @Index(name = "idx_notif_channel",   columnList = "channel"),
        @Index(name = "idx_notif_ref",       columnList = "reference_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Notification {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    // ── Recipient ─────────────────────────────────────────────────────────
    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 15)
    private String recipientPhone;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    // ── Notification metadata ─────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    // ── Reference to source entity (e.g. bookingId, catererId) ───────────
    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "reference_type", length = 30)
    private String referenceType;  // "BOOKING", "CATERER", "USER"

    // ── Status & retry ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    // ── Timestamps ────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Domain helpers ────────────────────────────────────────────────────
    public boolean canRetry() {
        return retryCount < maxRetries && status != NotificationStatus.SENT;
    }

    public void markSent() {
        this.status  = NotificationStatus.SENT;
        this.sentAt  = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.retryCount++;
        this.errorMessage = error;
        this.updatedAt = LocalDateTime.now();
        if (this.retryCount >= this.maxRetries) {
            this.status = NotificationStatus.FAILED;
        } else {
            this.status       = NotificationStatus.RETRYING;
            this.nextRetryAt  = LocalDateTime.now().plusSeconds(30L * retryCount);
        }
    }
}
