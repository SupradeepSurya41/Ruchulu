package com.ruchulu.notificationservice.repository;

import com.ruchulu.notificationservice.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndNotificationTypeOrderByCreatedAtDesc(
        String recipientId, NotificationType type, Pageable pageable);

    // Pending notifications for retry worker
    @Query("""
        SELECT n FROM Notification n
        WHERE n.status IN ('PENDING', 'RETRYING')
        AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now)
        ORDER BY n.createdAt ASC
    """)
    List<Notification> findPendingForDelivery(@Param("now") LocalDateTime now, Pageable pageable);

    // Failed notifications
    List<Notification> findByStatusAndRetryCountGreaterThanEqual(
        NotificationStatus status, Integer retryCount);

    // Stats
    long countByRecipientIdAndStatus(String recipientId, NotificationStatus status);
    long countByNotificationTypeAndStatus(NotificationType type, NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = 'FAILED' AND n.createdAt >= :since")
    long countRecentFailures(@Param("since") LocalDateTime since);
}
