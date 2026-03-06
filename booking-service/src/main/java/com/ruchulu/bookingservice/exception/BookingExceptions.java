package com.ruchulu.bookingservice.exception;

import org.springframework.http.HttpStatus;

/** Base exception */
public class BookingException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String     errorCode;
    public BookingException(String msg, HttpStatus s, String c) {
        super(msg); this.httpStatus = s; this.errorCode = c;
    }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getErrorCode()      { return errorCode; }
}

/** Booking not found */
class BookingNotFoundException extends BookingException {
    public BookingNotFoundException(String id) {
        super("Booking '" + id + "' not found.", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND");
    }
}

/** Invalid state transition attempted */
class InvalidBookingStateException extends BookingException {
    public InvalidBookingStateException(String from, String to) {
        super("Cannot transition booking from " + from + " to " + to + ".",
              HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION");
    }
}

/** Booking is already in a terminal state */
class BookingAlreadyTerminalException extends BookingException {
    public BookingAlreadyTerminalException(String status) {
        super("Booking is already " + status + " and cannot be modified.",
              HttpStatus.CONFLICT, "BOOKING_TERMINAL");
    }
}

/** Customer trying to book their own caterer profile */
class SelfBookingException extends BookingException {
    public SelfBookingException() {
        super("You cannot book your own catering service.",
              HttpStatus.BAD_REQUEST, "SELF_BOOKING_NOT_ALLOWED");
    }
}

/** Guest count outside caterer's capacity */
class GuestCountMismatchException extends BookingException {
    public GuestCountMismatchException(int requested, int min, int max) {
        super(String.format(
            "Requested %d guests but caterer serves %d–%d guests.",
            requested, min, max
        ), HttpStatus.BAD_REQUEST, "GUEST_COUNT_MISMATCH");
    }
}

/** Occasion not supported by the caterer */
class OccasionMismatchException extends BookingException {
    public OccasionMismatchException(String occasion) {
        super("This caterer does not serve " + occasion + " events.",
              HttpStatus.BAD_REQUEST, "OCCASION_MISMATCH");
    }
}

/** Event date too close — not enough advance notice */
class InsufficientAdvanceNoticeException extends BookingException {
    public InsufficientAdvanceNoticeException(int required) {
        super("This caterer requires at least " + required + " days advance booking notice.",
              HttpStatus.BAD_REQUEST, "INSUFFICIENT_ADVANCE_NOTICE");
    }
}

/** Cancellation window has passed */
class CancellationWindowExpiredException extends BookingException {
    public CancellationWindowExpiredException() {
        super("The cancellation window has passed. Bookings cannot be cancelled within 24 hours of the event.",
              HttpStatus.BAD_REQUEST, "CANCELLATION_WINDOW_EXPIRED");
    }
}

/** User is not authorised to access this booking */
class UnauthorizedBookingAccessException extends BookingException {
    public UnauthorizedBookingAccessException() {
        super("You do not have permission to access or modify this booking.",
              HttpStatus.FORBIDDEN, "BOOKING_ACCESS_DENIED");
    }
}

/** Caterer not approved — cannot accept bookings */
class CatererNotActiveException extends BookingException {
    public CatererNotActiveException(String catererId) {
        super("Caterer '" + catererId + "' is not currently active or approved.",
              HttpStatus.FORBIDDEN, "CATERER_NOT_ACTIVE");
    }
}

/** Payment amount mismatch */
class PaymentAmountMismatchException extends BookingException {
    public PaymentAmountMismatchException(String expected, String received) {
        super("Payment amount mismatch. Expected ₹" + expected + " but received ₹" + received + ".",
              HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH");
    }
}

/** Duplicate booking — same customer, caterer, date */
class DuplicateBookingException extends BookingException {
    public DuplicateBookingException() {
        super("A booking already exists for this caterer on the same date.",
              HttpStatus.CONFLICT, "DUPLICATE_BOOKING");
    }
}
