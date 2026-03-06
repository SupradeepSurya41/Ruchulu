package com.ruchulu.userservice.repository;

import com.ruchulu.userservice.model.AccountStatus;
import com.ruchulu.userservice.model.User;
import com.ruchulu.userservice.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // ── Find by unique identifiers ────────────────────────────────────
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByPhoneAndDeletedFalse(String phone);
    Optional<User> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT u FROM User u
        WHERE (LOWER(u.email) = LOWER(:id) OR u.phone = :id)
        AND u.deleted = false
    """)
    Optional<User> findByEmailOrPhone(@Param("id") String identifier);

    // ── Existence checks ──────────────────────────────────────────────
    boolean existsByEmailAndDeletedFalse(String email);
    boolean existsByPhoneAndDeletedFalse(String phone);

    // ── Token lookups ─────────────────────────────────────────────────
    Optional<User> findByEmailVerificationToken(String token);
    Optional<User> findByPasswordResetToken(String token);

    @Query("""
        SELECT u FROM User u
        WHERE u.passwordResetToken = :token
        AND u.passwordResetTokenExpiry > :now
        AND u.deleted = false
    """)
    Optional<User> findByValidPasswordResetToken(
        @Param("token") String token,
        @Param("now") LocalDateTime now
    );

    // ── Paginated queries ─────────────────────────────────────────────
    Page<User> findByRoleAndDeletedFalse(UserRole role, Pageable pageable);
    Page<User> findByAccountStatusAndDeletedFalse(AccountStatus status, Pageable pageable);
    Page<User> findByCityIgnoreCaseAndDeletedFalse(String city, Pageable pageable);

    @Query("""
        SELECT u FROM User u WHERE u.deleted = false
        AND (:city IS NULL OR LOWER(u.city) = LOWER(:city))
        AND (:role IS NULL OR u.role = :role)
        AND (:status IS NULL OR u.accountStatus = :status)
        ORDER BY u.createdAt DESC
    """)
    Page<User> findAllFiltered(
        @Param("city")   String city,
        @Param("role")   UserRole role,
        @Param("status") AccountStatus status,
        Pageable pageable
    );

    // ── Statistics ────────────────────────────────────────────────────
    long countByRoleAndDeletedFalse(UserRole role);
    long countByAccountStatusAndDeletedFalse(AccountStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since AND u.deleted = false")
    long countRegisteredSince(@Param("since") LocalDateTime since);

    // ── Mutations ─────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :at WHERE u.id = :id")
    void updateLastLoginAt(@Param("id") String id, @Param("at") LocalDateTime at);

    @Modifying
    @Query("""
        UPDATE User u
        SET u.emailVerified = true, u.emailVerificationToken = null,
            u.accountStatus = 'ACTIVE'
        WHERE u.id = :id
    """)
    void verifyEmail(@Param("id") String id);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus = :status WHERE u.id = :id")
    void updateAccountStatus(@Param("id") String id, @Param("status") AccountStatus status);

    // ── OTP ───────────────────────────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE User u
        SET u.otpCode = :code, u.otpExpiry = :expiry,
            u.otpAttempts = 0, u.otpLastSentAt = :sentAt
        WHERE u.id = :id
    """)
    void setOtp(
        @Param("id")     String id,
        @Param("code")   String code,
        @Param("expiry") LocalDateTime expiry,
        @Param("sentAt") LocalDateTime sentAt
    );

    @Modifying
    @Query("UPDATE User u SET u.otpCode = null, u.otpExpiry = null, u.otpAttempts = 0 WHERE u.id = :id")
    void clearOtp(@Param("id") String id);

    @Modifying
    @Query("UPDATE User u SET u.otpAttempts = u.otpAttempts + 1 WHERE u.id = :id")
    void incrementOtpAttempts(@Param("id") String id);

    // ── Search ────────────────────────────────────────────────────────
    @Query("""
        SELECT u FROM User u WHERE u.deleted = false
        AND (
            LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR
            LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:q,'%')) OR
            LOWER(u.email)     LIKE LOWER(CONCAT('%',:q,'%')) OR
            u.phone            LIKE CONCAT('%',:q,'%')
        )
        ORDER BY u.createdAt DESC
    """)
    List<User> searchUsers(@Param("q") String query, Pageable pageable);
}
