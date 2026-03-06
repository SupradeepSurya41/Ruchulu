package com.ruchulu.userservice.repository;

import com.ruchulu.userservice.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("h2")
@DisplayName("UserRepository — H2 Slice Tests")
class UserRepositoryTest {

    @Autowired private UserRepository repo;

    private User saved;

    @BeforeEach
    void setup() {
        saved = repo.save(User.builder()
                .firstName("Priya").lastName("Reddy")
                .email("priya@gmail.com").phone("9123456789")
                .passwordHash("$2a$12$hashed").role(UserRole.CUSTOMER)
                .city("Hyderabad").accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).emailVerificationToken("tok-abc")
                .build());
    }

    @AfterEach void cleanup() { repo.deleteAll(); }

    @Test @DisplayName("findByEmailAndDeletedFalse — found")
    void findByEmail_found() {
        assertThat(repo.findByEmailAndDeletedFalse("priya@gmail.com")).isPresent();
    }

    @Test @DisplayName("findByEmailAndDeletedFalse — not found for unknown email")
    void findByEmail_notFound() {
        assertThat(repo.findByEmailAndDeletedFalse("ghost@gmail.com")).isEmpty();
    }

    @Test @DisplayName("findByEmailAndDeletedFalse — returns empty for soft-deleted user")
    void findByEmail_softDeleted_empty() {
        saved.markDeleted();
        repo.save(saved);
        assertThat(repo.findByEmailAndDeletedFalse("priya@gmail.com")).isEmpty();
    }

    @Test @DisplayName("findByPhoneAndDeletedFalse — found")
    void findByPhone_found() {
        assertThat(repo.findByPhoneAndDeletedFalse("9123456789")).isPresent();
    }

    @Test @DisplayName("findByEmailOrPhone — matches by email")
    void findByEmailOrPhone_byEmail() {
        assertThat(repo.findByEmailOrPhone("priya@gmail.com")).isPresent();
    }

    @Test @DisplayName("findByEmailOrPhone — matches by phone")
    void findByEmailOrPhone_byPhone() {
        assertThat(repo.findByEmailOrPhone("9123456789")).isPresent();
    }

    @Test @DisplayName("existsByEmailAndDeletedFalse — true for existing")
    void existsByEmail_true() {
        assertThat(repo.existsByEmailAndDeletedFalse("priya@gmail.com")).isTrue();
    }

    @Test @DisplayName("existsByEmailAndDeletedFalse — false for unknown")
    void existsByEmail_false() {
        assertThat(repo.existsByEmailAndDeletedFalse("no@gmail.com")).isFalse();
    }

    @Test @DisplayName("existsByPhoneAndDeletedFalse — true for existing phone")
    void existsByPhone_true() {
        assertThat(repo.existsByPhoneAndDeletedFalse("9123456789")).isTrue();
    }

    @Test @DisplayName("findByEmailVerificationToken — found")
    void findByVerificationToken_found() {
        assertThat(repo.findByEmailVerificationToken("tok-abc")).isPresent();
    }

    @Test @DisplayName("findByEmailVerificationToken — not found for wrong token")
    void findByVerificationToken_notFound() {
        assertThat(repo.findByEmailVerificationToken("wrong-tok")).isEmpty();
    }

    @Test @DisplayName("verifyEmail — sets emailVerified=true, clears token, sets ACTIVE")
    void verifyEmail_updatesUser() {
        saved.setEmailVerified(false);
        saved.setEmailVerificationToken("verify-tok");
        saved.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        repo.save(saved);

        repo.verifyEmail(saved.getId());

        User u = repo.findById(saved.getId()).orElseThrow();
        assertThat(u.getEmailVerified()).isTrue();
        assertThat(u.getEmailVerificationToken()).isNull();
        assertThat(u.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test @DisplayName("countByRoleAndDeletedFalse — counts correctly")
    void countByRole_correct() {
        assertThat(repo.countByRoleAndDeletedFalse(UserRole.CUSTOMER)).isGreaterThanOrEqualTo(1);
        assertThat(repo.countByRoleAndDeletedFalse(UserRole.ADMIN)).isEqualTo(0);
    }

    @Test @DisplayName("updateLastLoginAt — updates the field")
    void updateLastLoginAt_works() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        repo.updateLastLoginAt(saved.getId(), now);
        User u = repo.findById(saved.getId()).orElseThrow();
        assertThat(u.getLastLoginAt()).isNotNull();
    }

    @Test @DisplayName("setOtp + clearOtp — sets and then clears OTP fields")
    void setAndClearOtp_works() {
        java.time.LocalDateTime expiry = java.time.LocalDateTime.now().plusMinutes(10);
        repo.setOtp(saved.getId(), "123456", expiry, java.time.LocalDateTime.now());

        User after = repo.findById(saved.getId()).orElseThrow();
        assertThat(after.getOtpCode()).isEqualTo("123456");
        assertThat(after.getOtpExpiry()).isNotNull();

        repo.clearOtp(saved.getId());
        User cleared = repo.findById(saved.getId()).orElseThrow();
        assertThat(cleared.getOtpCode()).isNull();
        assertThat(cleared.getOtpAttempts()).isEqualTo(0);
    }

    @Test @DisplayName("incrementOtpAttempts — increments by 1")
    void incrementOtpAttempts_works() {
        repo.incrementOtpAttempts(saved.getId());
        repo.incrementOtpAttempts(saved.getId());
        User u = repo.findById(saved.getId()).orElseThrow();
        assertThat(u.getOtpAttempts()).isEqualTo(2);
    }

    @Test @DisplayName("save — auto-assigns a UUID id")
    void save_assignsUuid() {
        assertThat(saved.getId()).isNotNull().isNotBlank();
    }

    @Test @DisplayName("findByRoleAndDeletedFalse — returns correct role")
    void findByRole_correct() {
        var page = repo.findByRoleAndDeletedFalse(UserRole.CUSTOMER, PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(u -> u.getRole() == UserRole.CUSTOMER);
    }
}
