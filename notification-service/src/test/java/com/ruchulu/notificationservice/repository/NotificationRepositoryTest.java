package com.ruchulu.notificationservice.repository;

import com.ruchulu.notificationservice.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("h2")
@DisplayName("NotificationRepository — H2 Slice Tests")
class NotificationRepositoryTest {

    @Autowired private NotificationRepository repo;

    private Notification saved;

    @BeforeEach
    void setup() {
        saved = repo.save(Notification.builder()
                .recipientId("usr-001").recipientEmail("ravi@gmail.com").recipientName("Ravi")
                .notificationType(NotificationType.BOOKING_CONFIRMED)
                .channel(NotificationChannel.EMAIL)
                .subject("Booking Confirmed").body("<html>...</html>")
                .status(NotificationStatus.PENDING)
                .retryCount(0).maxRetries(3)
                .build());
    }

    @AfterEach void cleanup() { repo.deleteAll(); }

    @Test @DisplayName("findByRecipientIdOrderByCreatedAtDesc — returns user's notifications")
    void findByRecipient_found() {
        var page = repo.findByRecipientIdOrderByCreatedAtDesc("usr-001", PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).getRecipientId()).isEqualTo("usr-001");
    }

    @Test @DisplayName("findPendingForDelivery — includes PENDING without nextRetryAt")
    void findPending_includesPending() {
        List<Notification> pending = repo.findPendingForDelivery(LocalDateTime.now(), PageRequest.of(0, 10));
        assertThat(pending).anyMatch(n -> n.getId().equals(saved.getId()));
    }

    @Test @DisplayName("findPendingForDelivery — excludes SENT notifications")
    void findPending_excludesSent() {
        saved.markSent();
        repo.save(saved);
        List<Notification> pending = repo.findPendingForDelivery(LocalDateTime.now(), PageRequest.of(0, 10));
        assertThat(pending).noneMatch(n -> n.getId().equals(saved.getId()));
    }

    @Test @DisplayName("countByRecipientIdAndStatus — counts correctly")
    void countByStatus_correct() {
        assertThat(repo.countByRecipientIdAndStatus("usr-001", NotificationStatus.PENDING))
            .isGreaterThanOrEqualTo(1);
    }

    @Test @DisplayName("countRecentFailures — returns 0 when none failed")
    void countRecentFailures_zero() {
        assertThat(repo.countRecentFailures(LocalDateTime.now().minusHours(1))).isEqualTo(0);
    }

    @Test @DisplayName("save — auto-assigns UUID id")
    void save_assignsUuid() {
        assertThat(saved.getId()).isNotNull().isNotBlank();
    }

    @Test @DisplayName("findByRecipientIdAndNotificationTypeOrderByCreatedAtDesc — type filter works")
    void findByTypeFilter_works() {
        var page = repo.findByRecipientIdAndNotificationTypeOrderByCreatedAtDesc(
            "usr-001", NotificationType.BOOKING_CONFIRMED, PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(n ->
            n.getNotificationType() == NotificationType.BOOKING_CONFIRMED);
    }
}
