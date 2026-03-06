package com.ruchulu.bookingservice.exception;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Booking Custom Exceptions — Status & Message Tests")
class BookingExceptionTest {

    @Test @DisplayName("BookingNotFoundException — 404, contains id")
    void bookingNotFound() {
        var ex = new BookingNotFoundException("bk-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("BOOKING_NOT_FOUND");
        assertThat(ex.getMessage()).contains("bk-001");
    }

    @Test @DisplayName("InvalidBookingStateException — 400, contains from/to states")
    void invalidState() {
        var ex = new InvalidBookingStateException("PENDING", "COMPLETED");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(ex.getMessage()).contains("PENDING").contains("COMPLETED");
    }

    @Test @DisplayName("BookingAlreadyTerminalException — 409, contains status")
    void alreadyTerminal() {
        var ex = new BookingAlreadyTerminalException("COMPLETED");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("BOOKING_TERMINAL");
        assertThat(ex.getMessage()).contains("COMPLETED");
    }

    @Test @DisplayName("SelfBookingException — 400")
    void selfBooking() {
        var ex = new SelfBookingException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("SELF_BOOKING_NOT_ALLOWED");
    }

    @Test @DisplayName("GuestCountMismatchException — 400, contains all three numbers")
    void guestCountMismatch() {
        var ex = new GuestCountMismatchException(800, 50, 500);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("GUEST_COUNT_MISMATCH");
        assertThat(ex.getMessage()).contains("800").contains("50").contains("500");
    }

    @Test @DisplayName("OccasionMismatchException — 400, contains occasion name")
    void occasionMismatch() {
        var ex = new OccasionMismatchException("FUNERAL");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("FUNERAL");
    }

    @Test @DisplayName("InsufficientAdvanceNoticeException — 400, contains required days")
    void insufficientNotice() {
        var ex = new InsufficientAdvanceNoticeException(3);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("3");
    }

    @Test @DisplayName("CancellationWindowExpiredException — 400")
    void cancellationWindowExpired() {
        var ex = new CancellationWindowExpiredException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("CANCELLATION_WINDOW_EXPIRED");
    }

    @Test @DisplayName("UnauthorizedBookingAccessException — 403")
    void unauthorizedAccess() {
        var ex = new UnauthorizedBookingAccessException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getErrorCode()).isEqualTo("BOOKING_ACCESS_DENIED");
    }

    @Test @DisplayName("CatererNotActiveException — 403, contains catererId")
    void catererNotActive() {
        var ex = new CatererNotActiveException("cat-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getMessage()).contains("cat-001");
    }

    @Test @DisplayName("PaymentAmountMismatchException — 400, contains both amounts")
    void paymentMismatch() {
        var ex = new PaymentAmountMismatchException("12000.00", "5000.00");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("12000.00").contains("5000.00");
    }

    @Test @DisplayName("DuplicateBookingException — 409")
    void duplicateBooking() {
        var ex = new DuplicateBookingException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("DUPLICATE_BOOKING");
    }
}
