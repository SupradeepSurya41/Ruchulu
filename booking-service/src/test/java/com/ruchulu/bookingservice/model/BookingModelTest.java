package com.ruchulu.bookingservice.model;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Booking Model — Field & State Machine Tests")
class BookingModelTest {

    // ── Defaults ───────────────────────────────────────────────────────────
    @Test @DisplayName("Default status is PENDING")
    void defaultStatus_pending() {
        assertThat(Booking.builder().build().getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test @DisplayName("Default paymentStatus is UNPAID")
    void defaultPaymentStatus_unpaid() {
        assertThat(Booking.builder().build().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test @DisplayName("Default deleted is false")
    void defaultDeleted_false() {
        assertThat(Booking.builder().build().getDeleted()).isFalse();
    }

    // ── isCancellable ──────────────────────────────────────────────────────
    @Test @DisplayName("isCancellable() — PENDING → true")
    void isCancellable_pending_true() {
        assertThat(booking(BookingStatus.PENDING).isCancellable()).isTrue();
    }

    @Test @DisplayName("isCancellable() — CONFIRMED → true")
    void isCancellable_confirmed_true() {
        assertThat(booking(BookingStatus.CONFIRMED).isCancellable()).isTrue();
    }

    @Test @DisplayName("isCancellable() — COMPLETED → false")
    void isCancellable_completed_false() {
        assertThat(booking(BookingStatus.COMPLETED).isCancellable()).isFalse();
    }

    @Test @DisplayName("isCancellable() — CANCELLED → false")
    void isCancellable_cancelled_false() {
        assertThat(booking(BookingStatus.CANCELLED).isCancellable()).isFalse();
    }

    // ── isTerminal ─────────────────────────────────────────────────────────
    @Test @DisplayName("isTerminal() — COMPLETED → true")
    void isTerminal_completed() {
        assertThat(booking(BookingStatus.COMPLETED).isTerminal()).isTrue();
    }

    @Test @DisplayName("isTerminal() — CANCELLED → true")
    void isTerminal_cancelled() {
        assertThat(booking(BookingStatus.CANCELLED).isTerminal()).isTrue();
    }

    @Test @DisplayName("isTerminal() — REJECTED → true")
    void isTerminal_rejected() {
        assertThat(booking(BookingStatus.REJECTED).isTerminal()).isTrue();
    }

    @Test @DisplayName("isTerminal() — EXPIRED → true")
    void isTerminal_expired() {
        assertThat(booking(BookingStatus.EXPIRED).isTerminal()).isTrue();
    }

    @Test @DisplayName("isTerminal() — PENDING → false")
    void isTerminal_pending_false() {
        assertThat(booking(BookingStatus.PENDING).isTerminal()).isFalse();
    }

    @Test @DisplayName("isTerminal() — CONFIRMED → false")
    void isTerminal_confirmed_false() {
        assertThat(booking(BookingStatus.CONFIRMED).isTerminal()).isFalse();
    }

    // ── isReviewable ───────────────────────────────────────────────────────
    @Test @DisplayName("isReviewable() — COMPLETED → true")
    void isReviewable_completed_true() {
        assertThat(booking(BookingStatus.COMPLETED).isReviewable()).isTrue();
    }

    @Test @DisplayName("isReviewable() — CONFIRMED → false")
    void isReviewable_confirmed_false() {
        assertThat(booking(BookingStatus.CONFIRMED).isReviewable()).isFalse();
    }

    // ── belongsToCustomer ─────────────────────────────────────────────────
    @Test @DisplayName("belongsToCustomer() — matching id → true")
    void belongsToCustomer_true() {
        Booking b = booking(BookingStatus.PENDING);
        b.setCustomerId("usr-001");
        assertThat(b.belongsToCustomer("usr-001")).isTrue();
    }

    @Test @DisplayName("belongsToCustomer() — different id → false")
    void belongsToCustomer_false() {
        Booking b = booking(BookingStatus.PENDING);
        b.setCustomerId("usr-001");
        assertThat(b.belongsToCustomer("usr-999")).isFalse();
    }

    // ── belongsToCaterer ──────────────────────────────────────────────────
    @Test @DisplayName("belongsToCaterer() — matching id → true")
    void belongsToCaterer_true() {
        Booking b = booking(BookingStatus.PENDING);
        b.setCatererId("cat-001");
        assertThat(b.belongsToCaterer("cat-001")).isTrue();
    }

    // ── calculateAmounts ──────────────────────────────────────────────────
    @Test @DisplayName("calculateAmounts() — correctly calculates total, advance, balance")
    void calculateAmounts_correct() {
        Booking b = booking(BookingStatus.PENDING);
        b.calculateAmounts(BigDecimal.valueOf(400), 100);

        assertThat(b.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(b.getAdvanceAmount()).isEqualByComparingTo(BigDecimal.valueOf(8000)); // 20%
        assertThat(b.getBalanceAmount()).isEqualByComparingTo(BigDecimal.valueOf(32000)); // 80%
        assertThat(b.getPricePerPlate()).isEqualByComparingTo(BigDecimal.valueOf(400));
    }

    @Test @DisplayName("calculateAmounts() — advance is exactly 20% of total")
    void calculateAmounts_advanceIs20Percent() {
        Booking b = booking(BookingStatus.PENDING);
        b.calculateAmounts(BigDecimal.valueOf(350), 200);

        BigDecimal expectedTotal   = BigDecimal.valueOf(70000);
        BigDecimal expectedAdvance = BigDecimal.valueOf(14000);
        BigDecimal expectedBalance = BigDecimal.valueOf(56000);

        assertThat(b.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(b.getAdvanceAmount()).isEqualByComparingTo(expectedAdvance);
        assertThat(b.getBalanceAmount()).isEqualByComparingTo(expectedBalance);
    }

    // ── Helper ────────────────────────────────────────────────────────────
    private Booking booking(BookingStatus status) {
        return Booking.builder()
                .id("bk-001")
                .customerId("usr-001").customerName("Ravi")
                .catererId("cat-001").catererName("Good Caterers")
                .occasion(OccasionType.WEDDING)
                .eventDate(LocalDate.now().plusDays(30))
                .eventCity("Hyderabad").eventAddress("Banjara Hills")
                .guestCount(200)
                .status(status)
                .deleted(false)
                .build();
    }
}
