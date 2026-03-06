package com.ruchulu.userservice.otp;

import com.ruchulu.userservice.exception.*;
import com.ruchulu.userservice.model.*;
import com.ruchulu.userservice.repository.OtpRecordRepository;
import com.ruchulu.userservice.repository.UserRepository;
import com.ruchulu.userservice.service.OtpService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService — Unit Tests")
class OtpServiceTest {

    @Mock private UserRepository      userRepository;
    @Mock private OtpRecordRepository otpRecordRepository;
    @Mock private JavaMailSender      mailSender;
    @Mock private PasswordEncoder     passwordEncoder;

    @InjectMocks private OtpService otpService;

    @BeforeEach
    void injectProps() {
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes",     10);
        ReflectionTestUtils.setField(otpService, "maxAttempts",          3);
        ReflectionTestUtils.setField(otpService, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(otpService, "fromEmail",            "test@ruchulu.com");
    }

    // ── generateOtp ───────────────────────────────────────────────────────
    @Test @DisplayName("generateOtp() — returns exactly 6 digits")
    void generateOtp_sixDigits() {
        String otp = otpService.generateOtp();
        assertThat(otp).matches("^\\d{6}$");
    }

    @Test @DisplayName("generateOtp() — is in range 100000–999999")
    void generateOtp_range() {
        for (int i = 0; i < 100; i++) {
            int val = Integer.parseInt(otpService.generateOtp());
            assertThat(val).isBetween(100000, 999999);
        }
    }

    @Test @DisplayName("generateOtp() — two consecutive OTPs differ (99.9999% of time)")
    void generateOtp_randomness() {
        // Run 10 times; at least one pair should differ
        java.util.Set<String> otps = new java.util.HashSet<>();
        for (int i = 0; i < 10; i++) otps.add(otpService.generateOtp());
        assertThat(otps.size()).isGreaterThan(1);
    }

    // ── generateAndSendOtp ────────────────────────────────────────────────
    @Test @DisplayName("generateAndSendOtp() — saves OTP and sends mail")
    void generateAndSend_success() throws Exception {
        User user = activeUser();
        user.setOtpLastSentAt(null); // no cooldown
        when(userRepository.findByIdAndDeletedFalse("usr-001")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed");
        when(otpRecordRepository.save(any())).thenReturn(new OtpRecord());

        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() ->
            otpService.generateAndSendOtp("usr-001", "ravi@gmail.com", OtpPurpose.LOGIN));

        verify(userRepository).setOtp(eq("usr-001"), anyString(), any(), any());
        verify(otpRecordRepository).save(any());
    }

    @Test @DisplayName("generateAndSendOtp() — resend within cooldown throws OtpResendTooSoonException")
    void generateAndSend_cooldown_throws() {
        User user = activeUser();
        user.setOtpLastSentAt(LocalDateTime.now().minusSeconds(30)); // 30s ago, cooldown is 60s
        when(userRepository.findByIdAndDeletedFalse("usr-001")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
            otpService.generateAndSendOtp("usr-001", "ravi@gmail.com", OtpPurpose.LOGIN))
            .isInstanceOf(OtpResendTooSoonException.class)
            .hasMessageContaining("30"); // ~30s remaining
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────
    @Test @DisplayName("verifyOtp() — correct code and not expired → succeeds")
    void verifyOtp_success() {
        User user = activeUser();
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setOtpAttempts(0);
        when(userRepository.findByIdAndDeletedFalse("usr-001")).thenReturn(Optional.of(user));

        assertThatNoException().isThrownBy(() ->
            otpService.verifyOtp("usr-001", "123456", OtpPurpose.LOGIN));

        verify(userRepository).clearOtp("usr-001");
    }

    @Test @DisplayName("verifyOtp() — expired OTP throws OtpExpiredException")
    void verifyOtp_expired_throws() {
        User user = activeUser();
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(1)); // expired
        user.setOtpAttempts(0);
        when(userRepository.findByIdAndDeletedFalse("usr-001")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
            otpService.verifyOtp("usr-001", "123456", OtpPurpose.LOGIN))
            .isInstanceOf(OtpExpiredException.class);

        verify(userRepository).clearOtp("usr-001");
    }

    @Test @DisplayName("verifyOtp() — wrong code increments attempts and throws InvalidOtpException")
    void verifyOtp_wrongCode_throws() {
        User user = activeUser();
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setOtpAttempts(0);
        when(userRepository.findByIdAndDeletedFalse("usr-001")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
            otpService.verifyOtp("usr-001", "000000", OtpPurpose.LOGIN))
            .isInstanceOf(InvalidOtpException.class);

        verify(userRepository).incrementOtpAttempts("usr-001");
    }

    @Test @DisplayName("verifyOtp() — max attempts exceeded throws OtpMaxAttemptsException")
    void verifyOtp_maxAttempts_throws() {
        User user = activeUser();
        user.setOtpAttempts(3); // already at max
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByIdAndDeletedFalse("usr-001")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
            otpService.verifyOtp("usr-001", "123456", OtpPurpose.LOGIN))
            .isInstanceOf(OtpMaxAttemptsException.class);

        verify(userRepository).clearOtp("usr-001");
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private User activeUser() {
        return User.builder()
                .id("usr-001").firstName("Ravi").lastName("Kumar")
                .email("ravi@gmail.com").phone("9876543210")
                .passwordHash("$hash").role(UserRole.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).deleted(false)
                .otpAttempts(0)
                .build();
    }
}
