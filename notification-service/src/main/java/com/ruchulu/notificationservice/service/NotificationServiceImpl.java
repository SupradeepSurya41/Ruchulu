package com.ruchulu.notificationservice.service;

import com.ruchulu.notificationservice.dto.*;
import com.ruchulu.notificationservice.exception.*;
import com.ruchulu.notificationservice.model.*;
import com.ruchulu.notificationservice.repository.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// ── Interface ─────────────────────────────────────────────────────────────
interface NotificationService {
    Notification send(SendNotificationRequest request);
    Notification sendOtp(OtpNotificationRequest request);
    void sendBookingNotifications(BookingNotificationRequest request);
    Page<Notification> getMyNotifications(String userId, int page, int size);
    void updatePreference(String userId, PreferenceUpdateRequest req);
    List<NotificationPreference> getPreferences(String userId);
    void retryFailed();
}

// ── Implementation ────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository           notifRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final EmailTemplateService             templateService;
    private final JavaMailSender                   mailSender;

    @Value("${app.notification.from-email}")
    private String fromEmail;

    @Value("${app.notification.retry-max-attempts:3}")
    private int maxRetries;

    // ── SEND GENERIC NOTIFICATION ─────────────────────────────────────────
    @Override
    public Notification send(SendNotificationRequest req) {
        log.info("Sending {} via {} to {}", req.getNotificationType(), req.getChannel(), req.getRecipientEmail());

        // Check user preference
        if (isOptedOut(req.getRecipientId(), req.getNotificationType(), req.getChannel())) {
            return saveSkipped(req);
        }

        String subject = templateService.buildSubject(req.getNotificationType(),
            req.getVariables() != null ? req.getVariables() : Map.of());
        String body = templateService.buildBody(req.getNotificationType(),
            req.getVariables() != null ? req.getVariables() : Map.of());

        Notification notif = Notification.builder()
                .recipientId(req.getRecipientId())
                .recipientEmail(req.getRecipientEmail())
                .recipientPhone(req.getRecipientPhone())
                .recipientName(req.getRecipientName())
                .notificationType(req.getNotificationType())
                .channel(req.getChannel())
                .subject(subject)
                .body(body)
                .referenceId(req.getReferenceId())
                .referenceType(req.getReferenceType())
                .status(NotificationStatus.PENDING)
                .maxRetries(maxRetries)
                .build();

        Notification saved = notifRepo.save(notif);

        // Attempt delivery
        if (req.getChannel() == NotificationChannel.EMAIL) {
            deliverEmail(saved);
        }
        return notifRepo.save(saved);
    }

    // ── SEND OTP ──────────────────────────────────────────────────────────
    @Override
    public Notification sendOtp(OtpNotificationRequest req) {
        NotificationType type = switch (req.getPurpose().toUpperCase()) {
            case "EMAIL_VERIFY"    -> NotificationType.OTP_EMAIL_VERIFY;
            case "PASSWORD_RESET"  -> NotificationType.OTP_PASSWORD_RESET;
            default                -> NotificationType.OTP_LOGIN;
        };

        return send(SendNotificationRequest.builder()
                .recipientId(req.getRecipientId())
                .recipientEmail(req.getRecipientEmail())
                .recipientName(req.getRecipientName())
                .notificationType(type)
                .channel(NotificationChannel.EMAIL)
                .variables(Map.of(
                    "name",           req.getRecipientName(),
                    "otp",            req.getOtpCode(),
                    "expiryMinutes",  String.valueOf(req.getExpiryMinutes() != null ? req.getExpiryMinutes() : 10)
                ))
                .build());
    }

    // ── SEND BOOKING NOTIFICATIONS (fires both customer + caterer emails) ──
    @Override
    public void sendBookingNotifications(BookingNotificationRequest req) {
        NotificationType custType = NotificationType.valueOf(req.getNotificationEvent());

        // Customer notification
        send(SendNotificationRequest.builder()
                .recipientId(req.getCustomerId())
                .recipientEmail(req.getCustomerEmail())
                .recipientName(req.getCustomerName())
                .notificationType(custType)
                .channel(NotificationChannel.EMAIL)
                .referenceId(req.getBookingId())
                .referenceType("BOOKING")
                .variables(buildBookingVars(req, true))
                .build());

        // Caterer notification for relevant events
        if (custType == NotificationType.BOOKING_CREATED) {
            send(SendNotificationRequest.builder()
                    .recipientId(req.getCatererId())
                    .recipientEmail(req.getCatererEmail())
                    .recipientName(req.getCatererName())
                    .notificationType(NotificationType.NEW_BOOKING_REQUEST)
                    .channel(NotificationChannel.EMAIL)
                    .referenceId(req.getBookingId())
                    .referenceType("BOOKING")
                    .variables(buildBookingVars(req, false))
                    .build());
        }
        if (custType == NotificationType.BOOKING_CANCELLED) {
            send(SendNotificationRequest.builder()
                    .recipientId(req.getCatererId())
                    .recipientEmail(req.getCatererEmail())
                    .recipientName(req.getCatererName())
                    .notificationType(NotificationType.BOOKING_CANCELLED_BY_CUSTOMER)
                    .channel(NotificationChannel.EMAIL)
                    .referenceId(req.getBookingId())
                    .referenceType("BOOKING")
                    .variables(buildBookingVars(req, false))
                    .build());
        }
    }

    // ── LIST MY NOTIFICATIONS ─────────────────────────────────────────────
    @Override
    public Page<Notification> getMyNotifications(String userId, int page, int size) {
        return notifRepo.findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    // ── PREFERENCES ───────────────────────────────────────────────────────
    @Override
    public void updatePreference(String userId, PreferenceUpdateRequest req) {
        Optional<NotificationPreference> existing = prefRepo
            .findByUserIdAndNotificationTypeAndChannel(userId, req.getNotificationType(), req.getChannel());
        if (existing.isPresent()) {
            existing.get().setEnabled(req.getEnabled());
            prefRepo.save(existing.get());
        } else {
            prefRepo.save(NotificationPreference.builder()
                    .userId(userId)
                    .notificationType(req.getNotificationType())
                    .channel(req.getChannel())
                    .enabled(req.getEnabled())
                    .build());
        }
    }

    @Override
    public List<NotificationPreference> getPreferences(String userId) {
        return prefRepo.findByUserId(userId);
    }

    // ── RETRY FAILED (scheduled every 5 minutes) ──────────────────────────
    @Override
    @Scheduled(fixedDelay = 300000)
    public void retryFailed() {
        List<Notification> pending = notifRepo.findPendingForDelivery(
            LocalDateTime.now(), PageRequest.of(0, 50));
        pending.forEach(n -> {
            if (n.getChannel() == NotificationChannel.EMAIL) {
                deliverEmail(n);
                notifRepo.save(n);
            }
        });
        if (!pending.isEmpty()) {
            log.info("Retry worker processed {} notifications.", pending.size());
        }
    }

    // ── PRIVATE: EMAIL DELIVERY ───────────────────────────────────────────
    private void deliverEmail(Notification notif) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(notif.getRecipientEmail());
            helper.setSubject(notif.getSubject());
            helper.setText(notif.getBody(), true);
            mailSender.send(msg);
            notif.markSent();
            log.info("Email sent: type={}, to={}", notif.getNotificationType(), notif.getRecipientEmail());
        } catch (Exception e) {
            log.error("Email failed: type={}, to={}, error={}", notif.getNotificationType(),
                notif.getRecipientEmail(), e.getMessage());
            notif.markFailed(e.getMessage());
        }
    }

    // ── PRIVATE: OPT-OUT CHECK ────────────────────────────────────────────
    private boolean isOptedOut(String userId, NotificationType type, NotificationChannel channel) {
        // OTP and critical notifications cannot be opted out
        if (type == NotificationType.OTP_LOGIN ||
            type == NotificationType.OTP_EMAIL_VERIFY ||
            type == NotificationType.OTP_PASSWORD_RESET) {
            return false;
        }
        return !prefRepo.existsByUserIdAndNotificationTypeAndChannelAndEnabledTrue(userId, type, channel);
    }

    private Notification saveSkipped(SendNotificationRequest req) {
        return notifRepo.save(Notification.builder()
                .recipientId(req.getRecipientId())
                .recipientEmail(req.getRecipientEmail())
                .notificationType(req.getNotificationType())
                .channel(req.getChannel())
                .status(NotificationStatus.SKIPPED)
                .build());
    }

    private Map<String, String> buildBookingVars(BookingNotificationRequest req, boolean forCustomer) {
        return Map.of(
            "customerName", req.getCustomerName() != null ? req.getCustomerName() : "",
            "catererName",  req.getCatererName()  != null ? req.getCatererName()  : "",
            "occasion",     req.getOccasion()     != null ? req.getOccasion()     : "",
            "eventDate",    req.getEventDate()    != null ? req.getEventDate()    : "",
            "eventCity",    req.getEventCity()    != null ? req.getEventCity()    : "",
            "guestCount",   req.getGuestCount()   != null ? req.getGuestCount().toString() : "",
            "totalAmount",  req.getTotalAmount()  != null ? req.getTotalAmount()  : "",
            "bookingId",    req.getBookingId()    != null ? req.getBookingId()    : ""
        );
    }
}
