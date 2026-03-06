package com.ruchulu.bookingservice.service;

import com.ruchulu.bookingservice.dto.*;
import com.ruchulu.bookingservice.exception.*;
import com.ruchulu.bookingservice.model.*;
import com.ruchulu.bookingservice.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — State Machine Unit Tests (Mocked)")
class BookingServiceTest {

    @Mock private BookingRepository      bookingRepo;
    @Mock private BookingEventRepository eventRepo;

    @InjectMocks private BookingServiceImpl service;

    @BeforeEach
    void injectProps() {
        ReflectionTestUtils.setField(service, "autoExpireHours",          48);
        ReflectionTestUtils.setField(service, "cancellationWindowHours",  24);
        ReflectionTestUtils.setField(service, "minAdvanceDays",            1);
    }

    // ── CREATE BOOKING ─────────────────────────────────────────────────────
    @Test @DisplayName("createBooking() — success creates PENDING booking")
    void createBooking_success() {
        when(bookingRepo.existsActiveBooking(any(), any(), any())).thenReturn(false);
        when(bookingRepo.save(any())).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId("bk-new");
            return b;
        });
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = service.createBooking("usr-001", "Ravi", "ravi@gmail.com", "9876543210",
            buildCreateRequest());

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getGuestCount()).isEqualTo(200);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(60000)); // 300 × 200
        verify(bookingRepo).save(any());
        verify(eventRepo).save(any());
    }

    @Test @DisplayName("createBooking() — self-booking throws SelfBookingException")
    void createBooking_selfBooking_throws() {
        CreateBookingRequest req = buildCreateRequest();
        req.setCatererId("usr-001"); // same as customer
        assertThatThrownBy(() ->
            service.createBooking("usr-001", "Ravi", "ravi@gmail.com", "9876543210", req))
            .isInstanceOf(SelfBookingException.class);
    }

    @Test @DisplayName("createBooking() — past date throws InsufficientAdvanceNoticeException")
    void createBooking_pastDate_throws() {
        CreateBookingRequest req = buildCreateRequest();
        req.setEventDate(LocalDate.now().minusDays(1));
        assertThatThrownBy(() ->
            service.createBooking("usr-001", "Ravi", "", "", req))
            .isInstanceOf(InsufficientAdvanceNoticeException.class);
    }

    @Test @DisplayName("createBooking() — duplicate booking throws DuplicateBookingException")
    void createBooking_duplicate_throws() {
        when(bookingRepo.existsActiveBooking("usr-001", "cat-001",
            LocalDate.now().plusDays(10))).thenReturn(true);

        assertThatThrownBy(() ->
            service.createBooking("usr-001", "Ravi", "", "", buildCreateRequest()))
            .isInstanceOf(DuplicateBookingException.class);
    }

    // ── CONFIRM BOOKING ────────────────────────────────────────────────────
    @Test @DisplayName("confirmBooking() — PENDING → CONFIRMED")
    void confirmBooking_success() {
        Booking b = pendingBooking();
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));
        when(bookingRepo.save(any())).thenReturn(b);
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.confirmBooking("bk-001", "cat-001", "Ready to serve!");

        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(b.getCatererNotes()).isEqualTo("Ready to serve!");
        assertThat(b.getConfirmedAt()).isNotNull();
    }

    @Test @DisplayName("confirmBooking() — non-PENDING state throws InvalidBookingStateException")
    void confirmBooking_wrongState_throws() {
        Booking b = pendingBooking();
        b.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.confirmBooking("bk-001", "cat-001", null))
            .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test @DisplayName("confirmBooking() — wrong caterer throws UnauthorizedBookingAccessException")
    void confirmBooking_wrongCaterer_throws() {
        Booking b = pendingBooking();
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.confirmBooking("bk-001", "cat-WRONG", null))
            .isInstanceOf(UnauthorizedBookingAccessException.class);
    }

    // ── REJECT BOOKING ─────────────────────────────────────────────────────
    @Test @DisplayName("rejectBooking() — PENDING → REJECTED with reason")
    void rejectBooking_success() {
        Booking b = pendingBooking();
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));
        when(bookingRepo.save(any())).thenReturn(b);
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.rejectBooking("bk-001", "cat-001", "Date not available");

        assertThat(b.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(b.getRejectionReason()).isEqualTo("Date not available");
    }

    // ── CANCEL BOOKING ─────────────────────────────────────────────────────
    @Test @DisplayName("cancelBooking() — PENDING → CANCELLED by customer")
    void cancelBooking_pending_success() {
        Booking b = pendingBooking();
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));
        when(bookingRepo.save(any())).thenReturn(b);
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.cancelBooking("bk-001", "usr-001", "Changed plans");

        assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b.getCancellationReason()).isEqualTo("Changed plans");
        assertThat(b.getCancelledAt()).isNotNull();
    }

    @Test @DisplayName("cancelBooking() — COMPLETED booking throws BookingAlreadyTerminalException")
    void cancelBooking_terminal_throws() {
        Booking b = pendingBooking();
        b.setStatus(BookingStatus.COMPLETED);
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancelBooking("bk-001", "usr-001", "reason"))
            .isInstanceOf(BookingAlreadyTerminalException.class);
    }

    @Test @DisplayName("cancelBooking() — wrong customer throws UnauthorizedBookingAccessException")
    void cancelBooking_wrongCustomer_throws() {
        Booking b = pendingBooking();
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancelBooking("bk-001", "usr-WRONG", "reason"))
            .isInstanceOf(UnauthorizedBookingAccessException.class);
    }

    @Test @DisplayName("cancelBooking() — CONFIRMED within 24h window throws CancellationWindowExpiredException")
    void cancelBooking_withinWindow_throws() {
        Booking b = pendingBooking();
        b.setStatus(BookingStatus.CONFIRMED);
        b.setEventDate(LocalDate.now()); // event is today — within 24h window
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancelBooking("bk-001", "usr-001", "last minute"))
            .isInstanceOf(CancellationWindowExpiredException.class);
    }

    // ── MARK IN-PROGRESS ───────────────────────────────────────────────────
    @Test @DisplayName("markInProgress() — CONFIRMED → IN_PROGRESS")
    void markInProgress_success() {
        Booking b = pendingBooking();
        b.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));
        when(bookingRepo.save(any())).thenReturn(b);
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.markInProgress("bk-001", "cat-001");
        assertThat(b.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);
    }

    @Test @DisplayName("markInProgress() — non-CONFIRMED state throws InvalidBookingStateException")
    void markInProgress_wrongState_throws() {
        Booking b = pendingBooking(); // PENDING, not CONFIRMED
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.markInProgress("bk-001", "cat-001"))
            .isInstanceOf(InvalidBookingStateException.class);
    }

    // ── MARK COMPLETED ─────────────────────────────────────────────────────
    @Test @DisplayName("markCompleted() — IN_PROGRESS → COMPLETED")
    void markCompleted_success() {
        Booking b = pendingBooking();
        b.setStatus(BookingStatus.IN_PROGRESS);
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));
        when(bookingRepo.save(any())).thenReturn(b);
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.markCompleted("bk-001", "cat-001");

        assertThat(b.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(b.getCompletedAt()).isNotNull();
        assertThat(b.isReviewable()).isTrue();
    }

    @Test @DisplayName("markCompleted() — non-IN_PROGRESS state throws InvalidBookingStateException")
    void markCompleted_wrongState_throws() {
        Booking b = pendingBooking(); // PENDING
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.markCompleted("bk-001", "cat-001"))
            .isInstanceOf(InvalidBookingStateException.class);
    }

    // ── GET BOOKING ────────────────────────────────────────────────────────
    @Test @DisplayName("getBookingById() — customer can view own booking")
    void getBookingById_customer_success() {
        Booking b = pendingBooking();
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        Booking result = service.getBookingById("bk-001", "usr-001");
        assertThat(result.getId()).isEqualTo("bk-001");
    }

    @Test @DisplayName("getBookingById() — unrelated user throws UnauthorizedBookingAccessException")
    void getBookingById_unauthorized_throws() {
        Booking b = pendingBooking();
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.getBookingById("bk-001", "usr-STRANGER"))
            .isInstanceOf(UnauthorizedBookingAccessException.class);
    }

    @Test @DisplayName("getBookingById() — not found throws BookingNotFoundException")
    void getBookingById_notFound_throws() {
        when(bookingRepo.findByIdAndDeletedFalse("bk-xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBookingById("bk-xxx", "usr-001"))
            .isInstanceOf(BookingNotFoundException.class);
    }

    // ── RECORD PAYMENT ─────────────────────────────────────────────────────
    @Test @DisplayName("recordPayment() — advance payment updates status to ADVANCE_PAID")
    void recordPayment_advance_updatesStatus() {
        Booking b = pendingBooking();
        b.calculateAmounts(BigDecimal.valueOf(300), 200);
        when(bookingRepo.findByIdAndDeletedFalse("bk-001")).thenReturn(Optional.of(b));
        when(bookingRepo.save(any())).thenReturn(b);
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentRequest req = new PaymentRequest("bk-001",
            b.getAdvanceAmount(), "UPI", "TXN123", null);
        service.recordPayment("bk-001", "usr-001", req);

        assertThat(b.getPaymentStatus()).isEqualTo(PaymentStatus.ADVANCE_PAID);
    }

    // ── AUTO-EXPIRE ────────────────────────────────────────────────────────
    @Test @DisplayName("expirePendingBookings() — expires old PENDING bookings")
    void expirePending_expires() {
        Booking b = pendingBooking();
        b.setExpiresAt(LocalDateTime.now().minusHours(1)); // already expired
        when(bookingRepo.findExpiredPendingBookings(any())).thenReturn(List.of(b));
        when(bookingRepo.save(any())).thenReturn(b);
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.expirePendingBookings();

        assertThat(b.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    // ── TEST DATA ─────────────────────────────────────────────────────────
    private Booking pendingBooking() {
        Booking b = Booking.builder()
                .id("bk-001")
                .customerId("usr-001").customerName("Ravi")
                .customerEmail("ravi@gmail.com").customerPhone("9876543210")
                .catererId("cat-001").catererName("Good Caterers")
                .occasion(OccasionType.WEDDING)
                .eventDate(LocalDate.now().plusDays(30))
                .eventCity("Hyderabad").eventAddress("Banjara Hills")
                .guestCount(200)
                .pricePerPlate(BigDecimal.valueOf(300))
                .totalAmount(BigDecimal.valueOf(60000))
                .advanceAmount(BigDecimal.valueOf(12000))
                .balanceAmount(BigDecimal.valueOf(48000))
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .deleted(false)
                .build();
        return b;
    }

    private CreateBookingRequest buildCreateRequest() {
        return CreateBookingRequest.builder()
                .catererId("cat-001")
                .occasion(OccasionType.WEDDING)
                .eventDate(LocalDate.now().plusDays(10))
                .eventCity("Hyderabad")
                .eventAddress("12-3-456, Banjara Hills, Hyderabad")
                .guestCount(200)
                .specialRequests("No pork")
                .build();
    }
}
