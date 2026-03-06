package com.ruchulu.userservice.repository;

import com.ruchulu.userservice.model.OtpPurpose;
import com.ruchulu.userservice.model.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecord, String> {

    List<OtpRecord> findByUserIdAndPurposeOrderByCreatedAtDesc(String userId, OtpPurpose purpose);

    Optional<OtpRecord> findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
        String userId, OtpPurpose purpose
    );

    /** Count OTPs sent to a user for a purpose within a time window (rate limiting) */
    @Query("""
        SELECT COUNT(r) FROM OtpRecord r
        WHERE r.userId = :userId
        AND r.purpose = :purpose
        AND r.createdAt >= :since
    """)
    long countRecentOtps(
        @Param("userId")  String userId,
        @Param("purpose") OtpPurpose purpose,
        @Param("since")   LocalDateTime since
    );

    /** Delete expired OTP records older than a threshold (cleanup job) */
    @Modifying
    @Query("DELETE FROM OtpRecord r WHERE r.expiresAt < :threshold")
    void deleteExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
