package com.ruchulu.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Set;

/**
 * @ValidCity — only accepts cities in Ruchulu's service area
 * (Telangana and Andhra Pradesh).
 */
@Documented
@Constraint(validatedBy = ValidCity.CityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCity {

    String message() default
        "City not supported. Ruchulu currently operates in: Hyderabad, Secunderabad, Warangal, " +
        "Vijayawada, Visakhapatnam, Guntur, Tirupati, Karimnagar, Nellore, Nizamabad, Kurnool, Rajahmundry.";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class CityValidator implements ConstraintValidator<ValidCity, String> {

        private static final Set<String> SUPPORTED_CITIES = Set.of(
            "hyderabad", "secunderabad", "warangal",
            "vijayawada", "visakhapatnam", "guntur",
            "tirupati", "karimnagar", "nellore",
            "nizamabad", "kurnool", "rajahmundry"
        );

        @Override
        public boolean isValid(String city, ConstraintValidatorContext context) {
            if (city == null || city.isBlank()) {
                return true; // city is optional; controller can enforce if needed
            }
            return SUPPORTED_CITIES.contains(city.trim().toLowerCase());
        }
    }
}
