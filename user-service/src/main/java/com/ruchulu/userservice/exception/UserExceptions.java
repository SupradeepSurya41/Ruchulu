package com.ruchulu.userservice.exception;

import org.springframework.http.HttpStatus;

// ═══════════════════════════════════════════════════════════════════════
// USER EXCEPTIONS
// ═══════════════════════════════════════════════════════════════════════

/** Email already registered in the system */
public class EmailAlreadyExistsException extends RuchuluException {
    public EmailAlreadyExistsException(String email) {
        super(
            "An account with email '" + email + "' already exists. " +
            "Please log in or use a different email address.",
            HttpStatus.CONFLICT, "USER_EMAIL_DUPLICATE"
        );
    }
}

/** Phone number already registered */
public class PhoneAlreadyExistsException extends RuchuluException {
    public PhoneAlreadyExistsException(String phone) {
        super(
            "An account with phone number '" + phone + "' is already registered. " +
            "Please use a different number.",
            HttpStatus.CONFLICT, "USER_PHONE_DUPLICATE"
        );
    }
}

/** No user found by the given identifier (email / phone / id) */
public class UserNotFoundException extends RuchuluException {
    public UserNotFoundException(String identifier) {
        super(
            "No account found for '" + identifier + "'. " +
            "Please check your details or create a new account.",
            HttpStatus.NOT_FOUND, "USER_NOT_FOUND"
        );
    }
}

/** Wrong password or email provided at login */
public class InvalidCredentialsException extends RuchuluException {
    public InvalidCredentialsException() {
        super(
            "Invalid email/phone or password. Please check your credentials and try again.",
            HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS"
        );
    }
}

/** Account has been suspended by admin */
public class AccountSuspendedException extends RuchuluException {
    public AccountSuspendedException(String reason) {
        super(
            "Your account has been suspended. Reason: " + reason +
            ". Please contact support@ruchulu.com to resolve this.",
            HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED"
        );
    }
}

/** Account is permanently deleted */
public class AccountDeletedException extends RuchuluException {
    public AccountDeletedException() {
        super(
            "This account has been permanently deleted and cannot be recovered. " +
            "Please create a new account.",
            HttpStatus.GONE, "ACCOUNT_DELETED"
        );
    }
}

/** Account is deactivated (user self-deactivated) */
public class AccountDeactivatedException extends RuchuluException {
    public AccountDeactivatedException() {
        super(
            "Your account has been deactivated. " +
            "Contact support@ruchulu.com to reactivate it.",
            HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED"
        );
    }
}

/** Email is not yet verified */
public class EmailNotVerifiedException extends RuchuluException {
    public EmailNotVerifiedException(String email) {
        super(
            "Please verify your email '" + email + "' before logging in. " +
            "Check your inbox for the verification link.",
            HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED"
        );
    }
}

/** Entered OTP is wrong */
public class InvalidOtpException extends RuchuluException {
    public InvalidOtpException() {
        super(
            "The OTP you entered is incorrect. Please check and try again.",
            HttpStatus.BAD_REQUEST, "OTP_INVALID"
        );
    }
}

/** OTP has expired */
public class OtpExpiredException extends RuchuluException {
    public OtpExpiredException() {
        super(
            "Your OTP has expired. Please request a new one.",
            HttpStatus.BAD_REQUEST, "OTP_EXPIRED"
        );
    }
}

/** Too many wrong OTP attempts */
public class OtpMaxAttemptsException extends RuchuluException {
    public OtpMaxAttemptsException() {
        super(
            "Too many incorrect OTP attempts. Your OTP has been invalidated. " +
            "Please request a new OTP.",
            HttpStatus.TOO_MANY_REQUESTS, "OTP_MAX_ATTEMPTS"
        );
    }
}

/** OTP resend attempted too soon */
public class OtpResendTooSoonException extends RuchuluException {
    public OtpResendTooSoonException(long secondsLeft) {
        super(
            "Please wait " + secondsLeft + " seconds before requesting a new OTP.",
            HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_TOO_SOON"
        );
    }
}

/** Password and confirm password don't match */
public class PasswordMismatchException extends RuchuluException {
    public PasswordMismatchException() {
        super(
            "New password and confirmation password do not match. Please re-enter.",
            HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH"
        );
    }
}

/** Current password provided is wrong */
public class WrongCurrentPasswordException extends RuchuluException {
    public WrongCurrentPasswordException() {
        super(
            "The current password you entered is incorrect.",
            HttpStatus.BAD_REQUEST, "PASSWORD_WRONG_CURRENT"
        );
    }
}

/** Token (email verification / password reset) is invalid or expired */
public class InvalidTokenException extends RuchuluException {
    public InvalidTokenException(String tokenType) {
        super(
            "The " + tokenType + " link is invalid or has expired. " +
            "Please request a new one.",
            HttpStatus.BAD_REQUEST, "TOKEN_INVALID_OR_EXPIRED"
        );
    }
}

/** City is not in Ruchulu's service area */
public class InvalidCityException extends RuchuluException {
    public InvalidCityException(String city) {
        super(
            "'" + city + "' is not in Ruchulu's service area yet. " +
            "We currently operate in Hyderabad, Vijayawada, Visakhapatnam, and more AP/TS cities.",
            HttpStatus.BAD_REQUEST, "CITY_NOT_SUPPORTED"
        );
    }
}

/** A required field is missing */
public class MissingFieldException extends RuchuluException {
    public MissingFieldException(String fieldName) {
        super(
            "Required field '" + fieldName + "' is missing or empty.",
            HttpStatus.BAD_REQUEST, "FIELD_MISSING"
        );
    }
}

/** Generic resource not found */
public class ResourceNotFoundException extends RuchuluException {
    public ResourceNotFoundException(String resourceType, String id) {
        super(
            resourceType + " with ID '" + id + "' was not found.",
            HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND"
        );
    }
}

/** Disposable or temp email was used */
public class DisposableEmailException extends RuchuluException {
    public DisposableEmailException(String domain) {
        super(
            "Email domain '" + domain + "' is a known disposable/temporary email service " +
            "and is not allowed on Ruchulu. Please use your real Gmail or Outlook address.",
            HttpStatus.BAD_REQUEST, "EMAIL_DISPOSABLE_DOMAIN"
        );
    }
}
