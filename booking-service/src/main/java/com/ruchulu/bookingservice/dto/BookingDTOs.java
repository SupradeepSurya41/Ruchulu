package com.ruchulu.bookingservice.dto;

import com.ruchulu.bookingservice.model.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ═══════════════════════════════════════════════════════════════
// CREATE BOOKING REQUEST — maps to frontend "Book Now" action
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateBookingRequest {

    @NotBlank(message = "Caterer ID is required")
    private String catererId;

    @NotNull(message = "Occasion type is required")
    private OccasionType occasion;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be a future date")
    private LocalDate eventDate;

    @NotBlank(message = "Event city is required")
    @Size(max = 60)
    private String eventCity;

    @NotBlank(message = "Event address is required")
    @Size(min = 10, max = 300, message = "Address must be 10–300 characters")
    private String eventAddress;

    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "At least 1 guest is required")
    @Max(value = 10000, message = "Cannot exceed 10,000 guests")
    private Integer guestCount;

    @DecimalMin(value = "0.0", message = "Price per plate cannot be negative")
    private BigDecimal pricePerPlate;   // optional; service uses caterer's rate if null

    private List<String> selectedMenuItemIds;

    @Size(max = 1000, message = "Special requests cannot exceed 1000 characters")
    private String specialRequests;
}

// ═══════════════════════════════════════════════════════════════
// BOOKING RESPONSE — full booking detail
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class BookingResponse {
    private String          id;
    private String          customerId;
    private String          customerName;
    private String          customerEmail;
    private String          customerPhone;
    private String          catererId;
    private String          catererName;
    private OccasionType    occasion;
    private LocalDate       eventDate;
    private String          eventCity;
    private String          eventAddress;
    private Integer         guestCount;
    private BigDecimal      pricePerPlate;
    private BigDecimal      totalAmount;
    private BigDecimal      advanceAmount;
    private BigDecimal      balanceAmount;
    private BookingStatus   status;
    private PaymentStatus   paymentStatus;
    private String          specialRequests;
    private String          catererNotes;
    private String          cancellationReason;
    private String          rejectionReason;
    private List<String>    selectedMenuItemIds;
    private LocalDateTime   confirmedAt;
    private LocalDateTime   completedAt;
    private LocalDateTime   cancelledAt;
    private LocalDateTime   expiresAt;
    private LocalDateTime   createdAt;
    private List<BookingEventResponse> timeline;
}

// ═══════════════════════════════════════════════════════════════
// BOOKING SUMMARY — for listing views
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class BookingSummaryResponse {
    private String          id;
    private String          catererName;
    private String          customerName;
    private OccasionType    occasion;
    private LocalDate       eventDate;
    private String          eventCity;
    private Integer         guestCount;
    private BigDecimal      totalAmount;
    private BookingStatus   status;
    private PaymentStatus   paymentStatus;
    private LocalDateTime   createdAt;
}

// ═══════════════════════════════════════════════════════════════
// BOOKING EVENT (timeline entry) RESPONSE
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class BookingEventResponse {
    private String           id;
    private BookingEventType eventType;
    private String           description;
    private String           performedBy;
    private LocalDateTime    createdAt;
}

// ═══════════════════════════════════════════════════════════════
// CONFIRM BOOKING REQUEST (caterer action)
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class ConfirmBookingRequest {
    @Size(max = 500)
    private String catererNotes;
}

// ═══════════════════════════════════════════════════════════════
// CANCEL BOOKING REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class CancelBookingRequest {
    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 5, max = 500, message = "Reason must be 5–500 characters")
    private String reason;
}

// ═══════════════════════════════════════════════════════════════
// REJECT BOOKING REQUEST (caterer action)
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class RejectBookingRequest {
    @NotBlank(message = "Rejection reason is required")
    @Size(min = 5, max = 500)
    private String reason;
}

// ═══════════════════════════════════════════════════════════════
// PAYMENT REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class PaymentRequest {
    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least ₹1")
    private BigDecimal amount;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode;   // UPI, CARD, BANK_TRANSFER, CASH

    private String transactionId;
    private String notes;
}

// ═══════════════════════════════════════════════════════════════
// BOOKING FILTER / SEARCH REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class BookingFilterRequest {
    private BookingStatus status;
    private OccasionType  occasion;
    private String        city;
    private LocalDate     fromDate;
    private LocalDate     toDate;
    @Min(0) private Integer page;
    @Min(1) @Max(50) private Integer size;
    private String sortBy;
}

// ═══════════════════════════════════════════════════════════════
// GENERIC API RESPONSE
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class ApiResponse<T> {
    private boolean       success;
    private String        message;
    private T             data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return ApiResponse.<T>builder().success(true).message(msg).data(data)
                .timestamp(LocalDateTime.now()).build();
    }
    public static <T> ApiResponse<T> ok(String msg) { return ok(msg, null); }
    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.<T>builder().success(false).message(msg)
                .timestamp(LocalDateTime.now()).build();
    }
}
