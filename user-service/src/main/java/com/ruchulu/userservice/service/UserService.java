package com.ruchulu.userservice.service;

import com.ruchulu.userservice.dto.*;
import com.ruchulu.userservice.model.User;

public interface UserService {

    // Registration & Auth
    AuthResponse register(RegisterRequest request);
    AuthResponse loginStep1(LoginRequest request);     // validates creds, sends OTP
    AuthResponse loginStep2OtpVerify(OtpVerifyRequest request); // verifies OTP, returns JWT

    // Email verification
    void sendEmailVerificationOtp(String userId);
    void verifyEmailOtp(String userId, String otp);
    void verifyEmailToken(String token);               // link-based fallback

    // Profile
    User getUserById(String id);
    User getUserByEmail(String email);
    User updateProfile(String userId, UpdateProfileRequest request);

    // Password
    void changePassword(String userId, ChangePasswordRequest request);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword, String confirmPassword);

    // Account lifecycle
    void deactivateAccount(String userId);
    void deleteAccount(String userId);
}
