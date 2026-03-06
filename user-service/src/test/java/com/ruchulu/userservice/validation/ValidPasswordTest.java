package com.ruchulu.userservice.validation;

import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ValidPassword — Password Strength Tests")
class ValidPasswordTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            validator = f.getValidator();
        }
    }

    private static class PwHolder {
        @ValidPassword String password;
        PwHolder(String p) { this.password = p; }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Secure@1234",
        "Ruchulu#2026!",
        "MyP@ssw0rd",
        "India@Feast99",
        "Biryani$9876"
    })
    @DisplayName("Strong passwords — pass")
    void strongPasswords_pass(String pw) {
        assertThat(validator.validate(new PwHolder(pw))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "short1A!",           // only 8 chars (valid, should pass actually)
    })
    @DisplayName("Exactly 8-char strong password — passes")
    void eightChars_passes(String pw) {
        assertThat(validator.validate(new PwHolder(pw))).isEmpty();
    }

    @Test @DisplayName("Password without uppercase — fails")
    void noUppercase_fails() {
        assertThat(validator.validate(new PwHolder("nocaps@123"))).isNotEmpty();
    }

    @Test @DisplayName("Password without lowercase — fails")
    void noLowercase_fails() {
        assertThat(validator.validate(new PwHolder("NOCAPS@123"))).isNotEmpty();
    }

    @Test @DisplayName("Password without digit — fails")
    void noDigit_fails() {
        assertThat(validator.validate(new PwHolder("NoDigits@here"))).isNotEmpty();
    }

    @Test @DisplayName("Password without special char — fails")
    void noSpecialChar_fails() {
        assertThat(validator.validate(new PwHolder("NoSpecial1234"))).isNotEmpty();
    }

    @Test @DisplayName("Password with space — fails")
    void withSpace_fails() {
        assertThat(validator.validate(new PwHolder("Has Space@1A"))).isNotEmpty();
    }

    @Test @DisplayName("Password shorter than 8 chars — fails")
    void tooShort_fails() {
        assertThat(validator.validate(new PwHolder("Ab1!"))).isNotEmpty();
    }

    @Test @DisplayName("Password longer than 128 chars — fails")
    void tooLong_fails() {
        String pw = "Aa1!" + "x".repeat(125);
        assertThat(validator.validate(new PwHolder(pw))).isNotEmpty();
    }

    @Test @DisplayName("Null password — passes (let @NotBlank handle)")
    void null_passes() {
        assertThat(validator.validate(new PwHolder(null))).isEmpty();
    }

    @Test @DisplayName("Blank password — passes (let @NotBlank handle)")
    void blank_passes() {
        assertThat(validator.validate(new PwHolder("   "))).isEmpty();
    }
}
