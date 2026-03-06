package com.ruchulu.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @ValidPassword — enforces strong password policy:
 *   ✅ 8–128 characters
 *   ✅ At least 1 uppercase letter (A–Z)
 *   ✅ At least 1 lowercase letter (a–z)
 *   ✅ At least 1 digit (0–9)
 *   ✅ At least 1 special character from (@$!%*?&_#^~)
 *   ❌ No spaces allowed
 *   ❌ Cannot be a common weak password
 */
@Documented
@Constraint(validatedBy = ValidPassword.PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default
        "Password must be 8–128 characters and contain at least: " +
        "1 uppercase letter, 1 lowercase letter, 1 digit, and 1 special character (@$!%*?&_#)";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

        // Common weak passwords to reject outright
        private static final java.util.Set<String> COMMON_PASSWORDS = java.util.Set.of(
            "Password1!", "Password@1", "Test@1234", "Admin@123",
            "Welcome@1", "Ruchulu@1", "India@123", "Qwerty@1"
        );

        @Override
        public boolean isValid(String password, ConstraintValidatorContext context) {
            if (password == null || password.isBlank()) {
                return true; // @NotBlank handles this
            }

            List<String> failures = new ArrayList<>();

            if (password.length() < 8)               failures.add("at least 8 characters");
            if (password.length() > 128)              failures.add("no more than 128 characters");
            if (!password.matches(".*[A-Z].*"))       failures.add("at least 1 uppercase letter (A-Z)");
            if (!password.matches(".*[a-z].*"))       failures.add("at least 1 lowercase letter (a-z)");
            if (!password.matches(".*\\d.*"))         failures.add("at least 1 digit (0-9)");
            if (!password.matches(".*[@$!%*?&_#^~].*"))
                                                      failures.add("at least 1 special character (@$!%*?&_#)");
            if (password.contains(" "))               failures.add("no spaces");
            if (COMMON_PASSWORDS.contains(password))  failures.add("must not be a commonly used password");

            if (!failures.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Password is too weak. Requirements: " + String.join(", ", failures) + "."
                ).addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}
