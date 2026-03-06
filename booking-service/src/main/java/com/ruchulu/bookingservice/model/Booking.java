package com.ruchulu.bookingservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Booking — represents a catering booking by a customer.
 *
 * Contains a snapshot of caterer and customer info at booking time
 * so historical records remain intact even if profiles change.
 *
 * State machine transitions enforced in BookingService.
 */
@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_customer",   columnList = "customer_id"),
        @Index(name = "idx_booking_caterer",    columnList = "caterer_id"),
        @Index(name = "idx_booking_status",     columnList = "status"),
        @Index(name = "idx_booking_event_date", columnList = "event_date"),
        @Index(name = "idx_booking_city",       columnList = "event_city")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "events")
public class Booking {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    // ── Customer snapshot ─────────────────────────────────────────────────
    @NotBlank(message = "Customer ID is required")
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 15)
    private String customerPhone;

    // ── Caterer snapshot ──────────────────────────────────────────────────
    @NotBlank(message = "Caterer ID is required")
    @Column(name = "caterer_id", nullable = false, length = 36)
    private String catererId;

    @Column(name = "caterer_name", nullable = false, length = 120)
    private String catererName;

    // ── Event details ─────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "occasion", nullable = false, length = 30)
    private OccasionType occasion;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @NotBlank(message = "Event city is required")
    @Column(name = "event_city", nullable = false, length = 60)
    private String eventCity;

    @NotBlank(message = "Event address is required")
    @Size(max = 300)
    @Column(name = "event_address", nullable = false, length = 300)
    private String eventAddress;

    @Min(value = 1, message = "Guest count must be at least 1")
    @Max(value = 10000, message = "Guest count cannot exceed 10,000")
    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    // ── Pricing snapshot (fixed at booking time) ──────────────────────────
    @Column(name = "price_per_plate", precision = 10, scale = 2)
    private BigDecimal pricePerPlate;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "advance_amount", precision = 12, scale = 2)
    private BigDecimal advanceAmount;   // 20% of total

    @Column(name = "balance_amount", precision = 12, scale = 2)
    private BigDecimal balanceAmount;   // remaining 80%

    // ── Additional menu selections ─────────────────────────────────────────
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_menu_items", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "menu_item_id", length = 36)
    @Builder.Default
    private List<String> selectedMenuItemIds = new ArrayList<>();

    // ── Requests ──────────────────────────────────────────────────────────
    @Size(max = 1000)
    @Column(name = "special_requests", length = 1000)
    private String specialRequests;

    // ── State ─────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 25)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "caterer_notes", length = 500)
    private String catererNotes;

    // ── Audit trail ───────────────────────────────────────────────────────
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingEvent> events = new ArrayList<>();

    // ── Timestamps ────────────────────────────────────────────────────────
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;   // auto-expire if caterer doesn't respond

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Soft delete ───────────────────────────────────────────────────────
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // ── Domain helpers ────────────────────────────────────────────────────
    public boolean isCancellable() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    public boolean isTerminal() {
        return status == BookingStatus.COMPLETED
            || status == BookingStatus.CANCELLED
            || status == BookingStatus.REJECTED
            || status == BookingStatus.EXPIRED;
    }

    public boolean isReviewable() {
        return status == BookingStatus.COMPLETED;
    }

    public boolean belongsToCustomer(String userId) {
        return customerId.equals(userId);
    }

    public boolean belongsToCaterer(String catererId) {
        return this.catererId.equals(catererId);
    }

    /**
     * Calculate advance amount (20% of total).
     * Called once at booking creation time.
     */
    public void calculateAmounts(BigDecimal pricePerPlate, int guestCount) {
        this.pricePerPlate  = pricePerPlate;
        this.totalAmount    = pricePerPlate.multiply(BigDecimal.valueOf(guestCount));
        this.advanceAmount  = totalAmount.multiply(BigDecimal.valueOf(0.20))
                                         .setScale(2, java.math.RoundingMode.HALF_UP);
        this.balanceAmount  = totalAmount.subtract(advanceAmount);
    }
}
