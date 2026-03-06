package com.ruchulu.userservice.dto;

import com.ruchulu.userservice.model.UserRole;
import com.ruchulu.userservice.validation.ValidCity;
import com.ruchulu.userservice.validation.ValidGmailAddress;
import com.ruchulu.userservice.validation.ValidPassword;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for the "Create Account" button in signupModal.
 * All fields map directly to the HTML form inputs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2–50 characters")
    @Pattern(
        regexp = "^[a-zA-Z\\s'\\-]+$",
        message = "First name can only contain letters, spaces, hyphens or apostrophes"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2–50 characters")
    @Pattern(
        regexp = "^[a-zA-Z\\s'\\-]+$",
        message = "Last name can only contain letters, spaces, hyphens or apostrophes"
    )
    private String lastName;

    @NotBlank(message = "Email address is required")
    @ValidGmailAddress   // ← custom: only real Gmail, blocks temp/spam domains
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone must be a valid 10-digit Indian number starting with 6, 7, 8, or 9"
    )
    private String phone;

    @ValidCity           // ← custom: only supported AP/TS cities
    private String city;

    @NotBlank(message = "Password is required")
    @ValidPassword       // ← custom: strong password rules
    private String password;

    @NotNull(message = "Please select a role — Customer or Caterer")
    private UserRole role;
}
