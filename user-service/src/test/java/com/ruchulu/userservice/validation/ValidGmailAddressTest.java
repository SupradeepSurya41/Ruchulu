package com.ruchulu.userservice.validation;

import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ValidGmailAddress — Email Validator Tests")
class ValidGmailAddressTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            validator = f.getValidator();
        }
    }

    // ── Test holder ───────────────────────────────────────────────────────
    private static class EmailHolder {
        @ValidGmailAddress
        String email;
        EmailHolder(String e) { this.email = e; }
    }

    // ── VALID emails that should pass ─────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {
        "ravi@gmail.com",
        "priya.sharma@gmail.com",
        "user+tag@gmail.com",
        "test123@gmail.com",
        "user@outlook.com",
        "user@hotmail.com",
        "user@yahoo.com",
        "user@yahoo.in",
        "user@rediffmail.com",
        "user@protonmail.com",
        "user@icloud.com",
        "user@ruchulu.com"
    })
    @DisplayName("Valid trusted emails — pass")
    void validEmails_pass(String email) {
        Set<ConstraintViolation<EmailHolder>> v = validator.validate(new EmailHolder(email));
        assertThat(v).as("Expected no violations for: " + email).isEmpty();
    }

    // ── DISPOSABLE / SPAM emails that must be rejected ────────────────────
    @ParameterizedTest
    @ValueSource(strings = {
        "user@mailinator.com",
        "user@guerrillamail.com",
        "user@10minutemail.com",
        "user@tempmail.com",
        "user@yopmail.com",
        "user@trashmail.com",
        "user@maildrop.cc",
        "user@sharklasers.com",
        "user@spam4.me",
        "user@fakeinbox.com",
        "user@discard.email",
        "user@mailsac.com",
        "user@getnada.com"
    })
    @DisplayName("Disposable/temp email domains — rejected")
    void disposableEmails_rejected(String email) {
        Set<ConstraintViolation<EmailHolder>> v = validator.validate(new EmailHolder(email));
        assertThat(v).as("Expected violation for: " + email).isNotEmpty();
    }

    // ── Untrusted / unknown domains ───────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {
        "user@somecustomdomain.com",
        "user@randomsite.net",
        "user@unknownco.org"
    })
    @DisplayName("Unknown non-trusted domains — rejected")
    void unknownDomains_rejected(String email) {
        Set<ConstraintViolation<EmailHolder>> v = validator.validate(new EmailHolder(email));
        assertThat(v).isNotEmpty();
    }

    // ── Syntactically invalid emails ──────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {
        "notanemail",
        "double@@gmail.com",
        "@gmail.com",
        "user@",
        "user@.com"
    })
    @DisplayName("Syntactically invalid emails — rejected")
    void invalidSyntax_rejected(String email) {
        Set<ConstraintViolation<EmailHolder>> v = validator.validate(new EmailHolder(email));
        assertThat(v).isNotEmpty();
    }

    // ── Suspicious local parts ────────────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {
        "noreply@gmail.com",
        "donotreply@gmail.com",
        "spam@gmail.com",
        "fake@gmail.com"
    })
    @DisplayName("Suspicious local parts (noreply/spam/fake) — rejected")
    void suspiciousLocalParts_rejected(String email) {
        Set<ConstraintViolation<EmailHolder>> v = validator.validate(new EmailHolder(email));
        assertThat(v).isNotEmpty();
    }

    // ── Null / blank ──────────────────────────────────────────────────────
    @Test @DisplayName("Null email — passes (let @NotBlank handle it)")
    void null_passes() {
        Set<ConstraintViolation<EmailHolder>> v = validator.validate(new EmailHolder(null));
        assertThat(v).isEmpty();
    }

    @Test @DisplayName("Blank email — passes (let @NotBlank handle it)")
    void blank_passes() {
        Set<ConstraintViolation<EmailHolder>> v = validator.validate(new EmailHolder("  "));
        assertThat(v).isEmpty();
    }

    // ── Case insensitivity ────────────────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {"User@GMAIL.COM", "USER@Gmail.Com", "test@GMAIL.COM"})
    @DisplayName("Gmail addresses are case-insensitive — pass")
    void caseInsensitive_gmail_pass(String email) {
        Set<ConstraintViolation<EmailHolder>> v = validator.validate(new EmailHolder(email));
        assertThat(v).isEmpty();
    }
}
