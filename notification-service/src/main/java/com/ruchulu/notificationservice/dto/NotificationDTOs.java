package com.ruchulu.notificationservice.dto;

import com.ruchulu.notificationservice.model.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// SEND NOTIFICATION REQUEST — generic trigger request
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SendNotificationRequest {

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;

    @NotBlank(message = "Recipient email is required")
    @Email
    private String recipientEmail;

    private String recipientPhone;
    private String recipientName;

    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    // Template variables (e.g. {otp: "123456", catererName: "Good Caterers"})
    private Map<String, String> variables;

    // Optional reference to source entity
    private String referenceId;
    private String referenceType;
}

// ═══════════════════════════════════════════════════════════════
// BOOKING NOTIFICATION REQUEST — fires all relevant notifications
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class BookingNotificationRequest {
    private String bookingId;
    private String customerId;
    private String customerEmail;
    private String customerName;
    private String customerPhone;
    private String catererId;
    private String catererEmail;
    private String catererName;
    private String occasion;
    private String eventDate;
    private String eventCity;
    private Integer guestCount;
    private String totalAmount;
    private String notificationEvent;  // BOOKING_CREATED, BOOKING_CONFIRMED, etc.
}

// ═══════════════════════════════════════════════════════════════
// OTP NOTIFICATION REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class OtpNotificationRequest {
    @NotBlank private String recipientId;
    @NotBlank @Email private String recipientEmail;
    @NotBlank private String recipientName;
    @NotBlank private String otpCode;
    @NotBlank private String purpose;  // LOGIN, EMAIL_VERIFY, PASSWORD_RESET
    private Integer expiryMinutes;
}

// ═══════════════════════════════════════════════════════════════
// NOTIFICATION RESPONSE
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class NotificationResponse {
    private String             id;
    private String             recipientId;
    private String             recipientEmail;
    private NotificationType   notificationType;
    private NotificationChannel channel;
    private NotificationStatus status;
    private String             subject;
    private Integer            retryCount;
    private LocalDateTime      sentAt;
    private LocalDateTime      createdAt;
}

// ═══════════════════════════════════════════════════════════════
// PREFERENCE UPDATE REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class PreferenceUpdateRequest {
    @NotNull private NotificationType notificationType;
    @NotNull private NotificationChannel channel;
    @NotNull private Boolean enabled;
}

// ═══════════════════════════════════════════════════════════════
// GENERIC API RESPONSE
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return ApiResponse.<T>builder().success(true).message(msg).data(data)
                .timestamp(LocalDateTime.now()).build();
    }
    public static <T> ApiResponse<T> ok(String msg) { return ok(msg, null); }
}
