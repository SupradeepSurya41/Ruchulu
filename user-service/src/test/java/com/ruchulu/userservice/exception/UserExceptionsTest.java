package com.ruchulu.userservice.exception;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Custom Exceptions — Message & Status Tests")
class UserExceptionsTest {

    @Test @DisplayName("EmailAlreadyExistsException — 409 CONFLICT, message contains email")
    void emailAlreadyExists() {
        var ex = new EmailAlreadyExistsException("test@gmail.com");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("USER_EMAIL_DUPLICATE");
        assertThat(ex.getMessage()).contains("test@gmail.com");
    }

    @Test @DisplayName("PhoneAlreadyExistsException — 409 CONFLICT")
    void phoneAlreadyExists() {
        var ex = new PhoneAlreadyExistsException("9876543210");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("USER_PHONE_DUPLICATE");
        assertThat(ex.getMessage()).contains("9876543210");
    }

    @Test @DisplayName("UserNotFoundException — 404 NOT_FOUND")
    void userNotFound() {
        var ex = new UserNotFoundException("ghost@gmail.com");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(ex.getMessage()).contains("ghost@gmail.com");
    }

    @Test @DisplayName("InvalidCredentialsException — 401 UNAUTHORIZED")
    void invalidCredentials() {
        var ex = new InvalidCredentialsException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getErrorCode()).isEqualTo("AUTH_INVALID_CREDENTIALS");
    }

    @Test @DisplayName("AccountSuspendedException — 403 FORBIDDEN, contains reason")
    void accountSuspended() {
        var ex = new AccountSuspendedException("Violation of terms");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getMessage()).contains("Violation of terms");
    }

    @Test @DisplayName("AccountDeletedException — 410 GONE")
    void accountDeleted() {
        var ex = new AccountDeletedException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.GONE);
        assertThat(ex.getErrorCode()).isEqualTo("ACCOUNT_DELETED");
    }

    @Test @DisplayName("AccountDeactivatedException — 403 FORBIDDEN")
    void accountDeactivated() {
        var ex = new AccountDeactivatedException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getErrorCode()).isEqualTo("ACCOUNT_DEACTIVATED");
    }

    @Test @DisplayName("InvalidOtpException — 400 BAD_REQUEST")
    void invalidOtp() {
        var ex = new InvalidOtpException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("OTP_INVALID");
    }

    @Test @DisplayName("OtpExpiredException — 400 BAD_REQUEST")
    void otpExpired() {
        var ex = new OtpExpiredException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("OTP_EXPIRED");
    }

    @Test @DisplayName("OtpMaxAttemptsException — 429 TOO_MANY_REQUESTS")
    void otpMaxAttempts() {
        var ex = new OtpMaxAttemptsException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ex.getErrorCode()).isEqualTo("OTP_MAX_ATTEMPTS");
    }

    @Test @DisplayName("OtpResendTooSoonException — 429, includes seconds remaining")
    void otpResendTooSoon() {
        var ex = new OtpResendTooSoonException(45L);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ex.getMessage()).contains("45");
    }

    @Test @DisplayName("PasswordMismatchException — 400")
    void passwordMismatch() {
        var ex = new PasswordMismatchException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("PASSWORD_MISMATCH");
    }

    @Test @DisplayName("WrongCurrentPasswordException — 400")
    void wrongCurrentPassword() {
        var ex = new WrongCurrentPasswordException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("PASSWORD_WRONG_CURRENT");
    }

    @Test @DisplayName("InvalidTokenException — 400, includes token type")
    void invalidToken() {
        var ex = new InvalidTokenException("password reset");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("password reset");
    }

    @Test @DisplayName("DisposableEmailException — 400, includes domain")
    void disposableEmail() {
        var ex = new DisposableEmailException("mailinator.com");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("mailinator.com");
    }

    @Test @DisplayName("MissingFieldException — 400, includes field name")
    void missingField() {
        var ex = new MissingFieldException("email");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("email");
    }

    @Test @DisplayName("ResourceNotFoundException — 404, includes type and id")
    void resourceNotFound() {
        var ex = new ResourceNotFoundException("Booking", "BK-123");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).contains("Booking").contains("BK-123");
    }

    @Test @DisplayName("InvalidCityException — 400, includes city name")
    void invalidCity() {
        var ex = new InvalidCityException("Mumbai");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("Mumbai");
    }
}
