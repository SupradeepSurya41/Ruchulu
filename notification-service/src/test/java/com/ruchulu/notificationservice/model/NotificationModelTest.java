package com.ruchulu.notificationservice.model;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Notification Model Tests")
class NotificationModelTest {

    @Test @DisplayName("Default status is PENDING")
    void defaultStatus_pending() {
        assertThat(Notification.builder().build().getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test @DisplayName("Default retryCount is 0")
    void defaultRetryCount_zero() {
        assertThat(Notification.builder().build().getRetryCount()).isEqualTo(0);
    }

    @Test @DisplayName("Default maxRetries is 3")
    void defaultMaxRetries_three() {
        assertThat(Notification.builder().build().getMaxRetries()).isEqualTo(3);
    }

    @Test @DisplayName("markSent() — sets SENT status and sentAt timestamp")
    void markSent_setsFields() {
        Notification n = Notification.builder().retryCount(0).maxRetries(3).build();
        n.markSent();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isNotNull();
    }

    @Test @DisplayName("markFailed() — increments retryCount")
    void markFailed_incrementsRetry() {
        Notification n = Notification.builder().retryCount(0).maxRetries(3).build();
        n.markFailed("SMTP error");
        assertThat(n.getRetryCount()).isEqualTo(1);
        assertThat(n.getErrorMessage()).isEqualTo("SMTP error");
    }

    @Test @DisplayName("markFailed() — sets RETRYING when under max retries")
    void markFailed_retrying() {
        Notification n = Notification.builder().retryCount(0).maxRetries(3).build();
        n.markFailed("timeout");
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.RETRYING);
    }

    @Test @DisplayName("markFailed() — sets FAILED when at max retries")
    void markFailed_failed_atMax() {
        Notification n = Notification.builder().retryCount(2).maxRetries(3).build();
        n.markFailed("final error");
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.getRetryCount()).isEqualTo(3);
    }

    @Test @DisplayName("canRetry() — true when under maxRetries and not SENT")
    void canRetry_true() {
        Notification n = Notification.builder().retryCount(1).maxRetries(3)
            .status(NotificationStatus.RETRYING).build();
        assertThat(n.canRetry()).isTrue();
    }

    @Test @DisplayName("canRetry() — false when at maxRetries")
    void canRetry_false_atMax() {
        Notification n = Notification.builder().retryCount(3).maxRetries(3)
            .status(NotificationStatus.FAILED).build();
        assertThat(n.canRetry()).isFalse();
    }

    @Test @DisplayName("canRetry() — false when already SENT")
    void canRetry_false_sent() {
        Notification n = Notification.builder().retryCount(0).maxRetries(3)
            .status(NotificationStatus.SENT).build();
        assertThat(n.canRetry()).isFalse();
    }

    @Test @DisplayName("markFailed() — sets nextRetryAt after failure")
    void markFailed_setsNextRetryAt() {
        Notification n = Notification.builder().retryCount(0).maxRetries(3).build();
        n.markFailed("error");
        assertThat(n.getNextRetryAt()).isNotNull();
        assertThat(n.getNextRetryAt()).isAfter(java.time.LocalDateTime.now().minusSeconds(1));
    }
}
