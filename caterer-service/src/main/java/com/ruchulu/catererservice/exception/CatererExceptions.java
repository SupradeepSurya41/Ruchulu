package com.ruchulu.catererservice.exception;

import org.springframework.http.HttpStatus;

/** Base for all caterer-service exceptions */
public class CatererException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String     errorCode;

    public CatererException(String message, HttpStatus status, String code) {
        super(message); this.httpStatus = status; this.errorCode = code;
    }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getErrorCode()      { return errorCode; }
}

/** Caterer profile not found */
class CatererNotFoundException extends CatererException {
    public CatererNotFoundException(String id) {
        super("Caterer profile '" + id + "' not found.", HttpStatus.NOT_FOUND, "CATERER_NOT_FOUND");
    }
}

/** A caterer profile already exists for this user */
class CatererAlreadyExistsException extends CatererException {
    public CatererAlreadyExistsException(String userId) {
        super("A caterer profile already exists for user '" + userId + "'.",
              HttpStatus.CONFLICT, "CATERER_ALREADY_EXISTS");
    }
}

/** Caterer is not yet approved — cannot accept bookings */
class CatererNotApprovedException extends CatererException {
    public CatererNotApprovedException(String id) {
        super("Caterer '" + id + "' is not approved yet. Please wait for admin verification.",
              HttpStatus.FORBIDDEN, "CATERER_NOT_APPROVED");
    }
}

/** Caterer is suspended */
class CatererSuspendedException extends CatererException {
    public CatererSuspendedException() {
        super("This caterer's account has been suspended. Please contact support.",
              HttpStatus.FORBIDDEN, "CATERER_SUSPENDED");
    }
}

/** Guest count is outside the caterer's serving capacity */
class GuestCountOutOfRangeException extends CatererException {
    public GuestCountOutOfRangeException(int requested, int min, int max) {
        super(String.format(
            "This caterer serves %d–%d guests. You requested %d guests.",
            min, max, requested
        ), HttpStatus.BAD_REQUEST, "GUEST_COUNT_OUT_OF_RANGE");
    }
}

/** Caterer does not serve the requested occasion */
class OccasionNotSupportedException extends CatererException {
    public OccasionNotSupportedException(String occasion, String caterer) {
        super("Caterer '" + caterer + "' does not cater for " + occasion + " events.",
              HttpStatus.BAD_REQUEST, "OCCASION_NOT_SUPPORTED");
    }
}

/** Menu item not found */
class MenuItemNotFoundException extends CatererException {
    public MenuItemNotFoundException(String id) {
        super("Menu item '" + id + "' not found.", HttpStatus.NOT_FOUND, "MENU_ITEM_NOT_FOUND");
    }
}

/** Review already submitted for this booking */
class ReviewAlreadyExistsException extends CatererException {
    public ReviewAlreadyExistsException(String bookingId) {
        super("A review for booking '" + bookingId + "' already exists.",
              HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS");
    }
}

/** FSSAI number is invalid format */
class InvalidFssaiNumberException extends CatererException {
    public InvalidFssaiNumberException(String number) {
        super("FSSAI number '" + number + "' is invalid. It must be exactly 14 digits.",
              HttpStatus.BAD_REQUEST, "FSSAI_INVALID");
    }
}

/** City not in service area */
class CityNotSupportedException extends CatererException {
    public CityNotSupportedException(String city) {
        super("Ruchulu does not operate in '" + city + "' yet. "
              + "We currently serve Hyderabad, Vijayawada, Visakhapatnam and 9 more AP/TS cities.",
              HttpStatus.BAD_REQUEST, "CITY_NOT_SUPPORTED");
    }
}

/** Unauthorized — accessing another caterer's data */
class UnauthorizedCatererAccessException extends CatererException {
    public UnauthorizedCatererAccessException() {
        super("You are not authorized to modify this caterer profile.",
              HttpStatus.FORBIDDEN, "CATERER_ACCESS_DENIED");
    }
}

/** Not enough advance notice for booking date */
class InsufficientAdvanceNoticeException extends CatererException {
    public InsufficientAdvanceNoticeException(int required) {
        super("This caterer requires at least " + required + " days advance notice for bookings.",
              HttpStatus.BAD_REQUEST, "INSUFFICIENT_ADVANCE_NOTICE");
    }
}
