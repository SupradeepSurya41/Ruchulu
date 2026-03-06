package com.ruchulu.userservice.service;

import com.ruchulu.userservice.config.JwtService;
import com.ruchulu.userservice.dto.*;
import com.ruchulu.userservice.exception.*;
import com.ruchulu.userservice.model.*;
import com.ruchulu.userservice.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Unit Tests (Mocked)")
class UserServiceTest {

    @Mock private UserRepository  userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService      jwtService;
    @Mock private OtpService      otpService;

    @InjectMocks private UserServiceImpl userService;

    // ── REGISTER ──────────────────────────────────────────────────────────
    @Test @DisplayName("register() — success creates user and returns tokens")
    void register_success() {
        RegisterRequest req = registerRequest();

        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneAndDeletedFalse(anyString())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("usr-test-001");
            return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access-tok");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-tok");
        doNothing().when(otpService).generateAndSendOtp(any(), any(), any());

        AuthResponse res = userService.register(req);

        assertThat(res.getAccessToken()).isEqualTo("access-tok");
        assertThat(res.getRefreshToken()).isEqualTo("refresh-tok");
        assertThat(res.getEmail()).isEqualTo("ravi@gmail.com");
        assertThat(res.getRole()).isEqualTo(UserRole.CUSTOMER);
        verify(userRepository).save(any(User.class));
    }

    @Test @DisplayName("register() — duplicate email throws EmailAlreadyExistsException")
    void register_duplicateEmail_throws() {
        RegisterRequest req = registerRequest();
        when(userRepository.existsByEmailAndDeletedFalse("ravi@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining("ravi@gmail.com");
    }

    @Test @DisplayName("register() — duplicate phone throws PhoneAlreadyExistsException")
    void register_duplicatePhone_throws() {
        RegisterRequest req = registerRequest();
        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneAndDeletedFalse("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
            .isInstanceOf(PhoneAlreadyExistsException.class)
            .hasMessageContaining("9876543210");
    }

    // ── LOGIN STEP 1 ──────────────────────────────────────────────────────
    @Test @DisplayName("loginStep1() — valid creds → OTP sent, otpRequired=true")
    void loginStep1_validCreds_sendsOtp() {
        User user = activeUser();
        LoginRequest req = new LoginRequest("ravi@gmail.com", "Secure@1234");

        when(userRepository.findByEmailOrPhone("ravi@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPasswordHash())).thenReturn(true);
        doNothing().when(otpService).generateAndSendOtp(any(), any(), any());

        AuthResponse res = userService.loginStep1(req);

        assertThat(res.isOtpRequired()).isTrue();
        assertThat(res.getMessage()).contains("OTP sent");
        verify(otpService).generateAndSendOtp(eq(user.getId()), eq(user.getEmail()), eq(OtpPurpose.LOGIN));
    }

    @Test @DisplayName("loginStep1() — wrong password throws InvalidCredentialsException")
    void loginStep1_wrongPwd_throws() {
        User user = activeUser();
        when(userRepository.findByEmailOrPhone(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> userService.loginStep1(new LoginRequest("ravi@gmail.com", "Wrong@1")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test @DisplayName("loginStep1() — unknown user throws InvalidCredentialsException")
    void loginStep1_unknownUser_throws() {
        when(userRepository.findByEmailOrPhone(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loginStep1(new LoginRequest("ghost@gmail.com", "X")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test @DisplayName("loginStep1() — deleted user throws AccountDeletedException")
    void loginStep1_deletedUser_throws() {
        User user = activeUser(); user.setDeleted(true);
        when(userRepository.findByEmailOrPhone(any())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.loginStep1(new LoginRequest("ravi@gmail.com", "pw")))
            .isInstanceOf(AccountDeletedException.class);
    }

    @Test @DisplayName("loginStep1() — suspended user throws AccountSuspendedException")
    void loginStep1_suspended_throws() {
        User user = activeUser(); user.setAccountStatus(AccountStatus.SUSPENDED);
        when(userRepository.findByEmailOrPhone(any())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.loginStep1(new LoginRequest("ravi@gmail.com", "pw")))
            .isInstanceOf(AccountSuspendedException.class);
    }

    // ── GET USER ──────────────────────────────────────────────────────────
    @Test @DisplayName("getUserById() — found returns user")
    void getUserById_found() {
        User user = activeUser();
        when(userRepository.findByIdAndDeletedFalse("usr-001")).thenReturn(Optional.of(user));

        User result = userService.getUserById("usr-001");
        assertThat(result.getEmail()).isEqualTo("ravi@gmail.com");
    }

    @Test @DisplayName("getUserById() — not found throws UserNotFoundException")
    void getUserById_notFound_throws() {
        when(userRepository.findByIdAndDeletedFalse("xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("xxx"))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("xxx");
    }

    // ── CHANGE PASSWORD ───────────────────────────────────────────────────
    @Test @DisplayName("changePassword() — wrong current password throws")
    void changePassword_wrongCurrent_throws() {
        User user = activeUser();
        when(userRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong@1", user.getPasswordHash())).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest("Wrong@1", "New@Pass1", "New@Pass1");
        assertThatThrownBy(() -> userService.changePassword(user.getId(), req))
            .isInstanceOf(WrongCurrentPasswordException.class);
    }

    @Test @DisplayName("changePassword() — mismatched new passwords throws PasswordMismatchException")
    void changePassword_mismatch_throws() {
        User user = activeUser();
        when(userRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest("Current@1", "New@Pass1", "Different@1");
        assertThatThrownBy(() -> userService.changePassword(user.getId(), req))
            .isInstanceOf(PasswordMismatchException.class);
    }

    @Test @DisplayName("changePassword() — success updates passwordHash")
    void changePassword_success() {
        User user = activeUser();
        when(userRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current@1", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("New@Pass1")).thenReturn("$new$hashed");
        when(userRepository.save(any())).thenReturn(user);

        ChangePasswordRequest req = new ChangePasswordRequest("Current@1", "New@Pass1", "New@Pass1");
        assertThatNoException().isThrownBy(() -> userService.changePassword(user.getId(), req));
        assertThat(user.getPasswordHash()).isEqualTo("$new$hashed");
    }

    // ── DELETE / DEACTIVATE ───────────────────────────────────────────────
    @Test @DisplayName("deleteAccount() — marks user as deleted")
    void deleteAccount_setsDeleted() {
        User user = activeUser();
        when(userRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.deleteAccount(user.getId());

        assertThat(user.getDeleted()).isTrue();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.DELETED);
    }

    @Test @DisplayName("deactivateAccount() — sets status to DEACTIVATED")
    void deactivateAccount_setsStatus() {
        User user = activeUser();
        when(userRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.deactivateAccount(user.getId());

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.DEACTIVATED);
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────────
    @Test @DisplayName("resetPassword() — mismatched passwords throws PasswordMismatchException")
    void resetPassword_mismatch_throws() {
        assertThatThrownBy(() ->
            userService.resetPassword("tok", "New@Pass1", "Diff@Pass1"))
            .isInstanceOf(PasswordMismatchException.class);
    }

    // ── TEST DATA ─────────────────────────────────────────────────────────
    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .firstName("Ravi").lastName("Kumar")
                .email("ravi@gmail.com").phone("9876543210")
                .password("Secure@1234").city("Hyderabad")
                .role(UserRole.CUSTOMER).build();
    }

    private User activeUser() {
        return User.builder()
                .id("usr-001").firstName("Ravi").lastName("Kumar")
                .email("ravi@gmail.com").phone("9876543210")
                .passwordHash("$2a$12$hashed").role(UserRole.CUSTOMER)
                .city("Hyderabad").accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).deleted(false).build();
    }
}
