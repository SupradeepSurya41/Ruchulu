package com.ruchulu.notificationservice.repository;

import com.ruchulu.notificationservice.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
    List<NotificationPreference> findByUserId(String userId);
    Optional<NotificationPreference> findByUserIdAndNotificationTypeAndChannel(
        String userId, NotificationType type, NotificationChannel channel);
    boolean existsByUserIdAndNotificationTypeAndChannelAndEnabledTrue(
        String userId, NotificationType type, NotificationChannel channel);
}
