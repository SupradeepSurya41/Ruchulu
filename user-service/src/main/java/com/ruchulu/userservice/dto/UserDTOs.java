package com.ruchulu.userservice.dto;

import com.ruchulu.userservice.model.AccountStatus;
import com.ruchulu.userservice.model.AuthProvider;
import com.ruchulu.userservice.model.UserRole;
import com.ruchulu.userservice.validation.ValidGmailAddress;
import com.ruchulu.userservice.validation.ValidPassword;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

// ═══════════════════════════════════════════════════════════════
// LOGIN REQUEST — Step 1: submit email + password → get OTP sent
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class LoginRequest {

    @NotBlank(message = "Email or phone is required")
    @Size(max = 150, message = "Identifier too long")
    private String identifier;          // email OR phone from loginModal

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be 8–128 characters")
    private String password;
}

// ═══════════════════════════════════════════════════════════════
// OTP VERIFY REQUEST — Step 2: submit OTP to complete login
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class OtpVerifyRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must contain only digits")
    private String otp;

    @NotBlank(message = "Purpose is required")
    private String purpose;              // LOGIN, EMAIL_VERIFICATION, PASSWORD_RESET
}

// ═══════════════════════════════════════════════════════════════
// OTP RESEND REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class OtpResendRequest {

    @NotBlank(message = "Email is required")
    @ValidGmailAddress
    private String email;

    @NotBlank(message = "Purpose is required")
    private String purpose;
}

// ═══════════════════════════════════════════════════════════════
// AUTH RESPONSE — returned after login or register
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class AuthResponse {
    private String  accessToken;
    private String  refreshToken;
    @Builder.Default
    private String  tokenType  = "Bearer";
    private Long    expiresIn;            // seconds
    private String  userId;
    private String  firstName;
    private String  lastName;
    private String  email;
    private String  phone;
    private String  city;
    private UserRole role;
    private Boolean emailVerified;
    private String  profilePictureUrl;
    private String  message;
    private boolean otpRequired;          // true = OTP step needed
}

// ═══════════════════════════════════════════════════════════════
// USER PROFILE RESPONSE
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class UserProfileResponse {
    private String        id;
    private String        firstName;
    private String        lastName;
    private String        fullName;
    private String        email;
    private String        phone;
    private String        city;
    private UserRole      role;
    private AccountStatus accountStatus;
    private Boolean       emailVerified;
    private Boolean       phoneVerified;
    private AuthProvider  authProvider;
    private String        profilePictureUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}

// ═══════════════════════════════════════════════════════════════
// UPDATE PROFILE REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "First name must be 2–50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'\\-]+$", message = "First name contains invalid characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be 2–50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'\\-]+$", message = "Last name contains invalid characters")
    private String lastName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian number")
    private String phone;

    @Size(max = 60, message = "City name is too long")
    private String city;

    @Size(max = 500, message = "Profile picture URL is too long")
    private String profilePictureUrl;
}

// ═══════════════════════════════════════════════════════════════
// CHANGE PASSWORD REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}

// ═══════════════════════════════════════════════════════════════
// FORGOT PASSWORD REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @ValidGmailAddress
    private String email;
}

// ═══════════════════════════════════════════════════════════════
// RESET PASSWORD REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}

// ═══════════════════════════════════════════════════════════════
// GENERIC API RESPONSE WRAPPER
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class ApiResponse<T> {
    private boolean       success;
    private String        message;
    private T             data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true).message(message).data(data)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> ok(String message) {
        return ok(message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false).message(message)
                .timestamp(LocalDateTime.now()).build();
    }
}
