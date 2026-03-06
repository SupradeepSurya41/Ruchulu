package com.ruchulu.userservice.model;

public enum OtpPurpose {
    LOGIN,              // OTP sent on login step 2
    EMAIL_VERIFICATION, // verify email after registration
    PASSWORD_RESET,     // forgot password flow
    PHONE_VERIFICATION  // verify phone number
}
