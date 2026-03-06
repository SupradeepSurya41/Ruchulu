package com.ruchulu.userservice.model;

import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Model — Field & Behaviour Tests")
class UserModelTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            validator = f.getValidator();
        }
    }

    // ── Builder defaults ──────────────────────────────────────────────────
    @Test @DisplayName("Default role is CUSTOMER")
    void defaults_role() {
        User u = User.builder().build();
        assertThat(u.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test @DisplayName("Default accountStatus is PENDING_VERIFICATION")
    void defaults_accountStatus() {
        User u = User.builder().build();
        assertThat(u.getAccountStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test @DisplayName("Default emailVerified is false")
    void defaults_emailVerified() {
        User u = User.builder().build();
        assertThat(u.getEmailVerified()).isFalse();
    }

    @Test @DisplayName("Default deleted is false")
    void defaults_deleted() {
        User u = User.builder().build();
        assertThat(u.getDeleted()).isFalse();
    }

    @Test @DisplayName("Default authProvider is LOCAL")
    void defaults_authProvider() {
        User u = User.builder().build();
        assertThat(u.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
    }

    @Test @DisplayName("Default otpAttempts is 0")
    void defaults_otpAttempts() {
        User u = User.builder().build();
        assertThat(u.getOtpAttempts()).isEqualTo(0);
    }

    // ── firstName validation ──────────────────────────────────────────────
    @Test @DisplayName("firstName — null fails validation")
    void firstName_null_fails() {
        User u = validUser(); u.setFirstName(null);
        assertViolationOn(u, "firstName");
    }

    @Test @DisplayName("firstName — blank fails validation")
    void firstName_blank_fails() {
        User u = validUser(); u.setFirstName("   ");
        assertViolationOn(u, "firstName");
    }

    @ParameterizedTest
    @ValueSource(strings = {"R", "X"})
    @DisplayName("firstName — too short (1 char) fails")
    void firstName_tooShort_fails(String name) {
        User u = validUser(); u.setFirstName(name);
        assertViolationOn(u, "firstName");
    }

    @Test @DisplayName("firstName — 50 chars passes")
    void firstName_maxLength_passes() {
        User u = validUser(); u.setFirstName("A".repeat(50));
        assertNoViolationOn(u, "firstName");
    }

    @Test @DisplayName("firstName — 51 chars fails")
    void firstName_overMax_fails() {
        User u = validUser(); u.setFirstName("A".repeat(51));
        assertViolationOn(u, "firstName");
    }

    // ── email validation ──────────────────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {"ravi@gmail.com", "test.user+tag@gmail.com"})
    @DisplayName("email — valid formats pass")
    void email_valid_passes(String email) {
        User u = validUser(); u.setEmail(email);
        assertNoViolationOn(u, "email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"notanemail", "double@@test.com", "@no-local.com", "no-at-sign"})
    @DisplayName("email — invalid formats fail")
    void email_invalid_fails(String email) {
        User u = validUser(); u.setEmail(email);
        assertViolationOn(u, "email");
    }

    @Test @DisplayName("email — null fails")
    void email_null_fails() {
        User u = validUser(); u.setEmail(null);
        assertViolationOn(u, "email");
    }

    // ── phone validation ──────────────────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "8765432109", "7654321098", "6543210987"})
    @DisplayName("phone — valid Indian numbers pass")
    void phone_valid_passes(String phone) {
        User u = validUser(); u.setPhone(phone);
        assertNoViolationOn(u, "phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "5000000000", "987654321", "98765432100", "+919876543210"})
    @DisplayName("phone — invalid numbers fail")
    void phone_invalid_fails(String phone) {
        User u = validUser(); u.setPhone(phone);
        assertViolationOn(u, "phone");
    }

    @Test @DisplayName("phone — null fails")
    void phone_null_fails() {
        User u = validUser(); u.setPhone(null);
        assertViolationOn(u, "phone");
    }

    // ── Domain behaviour methods ──────────────────────────────────────────
    @Test @DisplayName("getFullName() — returns firstName space lastName")
    void getFullName_correct() {
        User u = validUser();
        u.setFirstName("Ravi"); u.setLastName("Kumar");
        assertThat(u.getFullName()).isEqualTo("Ravi Kumar");
    }

    @Test @DisplayName("isActive() — ACTIVE + deleted=false → true")
    void isActive_activeNotDeleted_true() {
        User u = validUser();
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setDeleted(false);
        assertThat(u.isActive()).isTrue();
    }

    @Test @DisplayName("isActive() — ACTIVE + deleted=true → false")
    void isActive_deleted_false() {
        User u = validUser();
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setDeleted(true);
        assertThat(u.isActive()).isFalse();
    }

    @Test @DisplayName("isActive() — SUSPENDED → false")
    void isActive_suspended_false() {
        User u = validUser();
        u.setAccountStatus(AccountStatus.SUSPENDED);
        assertThat(u.isActive()).isFalse();
    }

    @Test @DisplayName("markDeleted() sets deleted=true, status=DELETED, deletedAt set")
    void markDeleted_setsAllFields() {
        User u = validUser();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        u.markDeleted();
        assertThat(u.getDeleted()).isTrue();
        assertThat(u.getAccountStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(u.getDeletedAt()).isAfter(before);
    }

    @Test @DisplayName("clearOtp() nullifies otp fields and resets attempts")
    void clearOtp_resetsFields() {
        User u = validUser();
        u.setOtpCode("123456");
        u.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        u.setOtpAttempts(2);
        u.clearOtp();
        assertThat(u.getOtpCode()).isNull();
        assertThat(u.getOtpExpiry()).isNull();
        assertThat(u.getOtpAttempts()).isEqualTo(0);
    }

    @Test @DisplayName("isOtpValid() — matching code + not expired → true")
    void isOtpValid_validOtp_true() {
        User u = validUser();
        u.setOtpCode("654321");
        u.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        assertThat(u.isOtpValid("654321")).isTrue();
    }

    @Test @DisplayName("isOtpValid() — expired → false")
    void isOtpValid_expired_false() {
        User u = validUser();
        u.setOtpCode("654321");
        u.setOtpExpiry(LocalDateTime.now().minusMinutes(1));
        assertThat(u.isOtpValid("654321")).isFalse();
    }

    @Test @DisplayName("isOtpValid() — wrong code → false")
    void isOtpValid_wrongCode_false() {
        User u = validUser();
        u.setOtpCode("654321");
        u.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        assertThat(u.isOtpValid("000000")).isFalse();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private User validUser() {
        return User.builder()
                .firstName("Ravi").lastName("Kumar")
                .email("ravi@gmail.com").phone("9876543210")
                .passwordHash("$2a$12$hashed")
                .role(UserRole.CUSTOMER).city("Hyderabad")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).deleted(false)
                .build();
    }

    private void assertViolationOn(User u, String field) {
        Set<ConstraintViolation<User>> v = validator.validate(u);
        assertThat(v).anyMatch(cv -> cv.getPropertyPath().toString().equals(field));
    }

    private void assertNoViolationOn(User u, String field) {
        Set<ConstraintViolation<User>> v = validator.validate(u);
        assertThat(v).noneMatch(cv -> cv.getPropertyPath().toString().equals(field));
    }
}
