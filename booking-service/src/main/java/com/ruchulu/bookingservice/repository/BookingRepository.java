package com.ruchulu.bookingservice.repository;

import com.ruchulu.bookingservice.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // ── Customer queries ─────────────────────────────────────────────
    Page<Booking> findByCustomerIdAndDeletedFalse(String customerId, Pageable pageable);

    Page<Booking> findByCustomerIdAndStatusAndDeletedFalse(
        String customerId, BookingStatus status, Pageable pageable
    );

    Optional<Booking> findByIdAndDeletedFalse(String id);

    // ── Caterer queries ───────────────────────────────────────────────
    Page<Booking> findByCatererIdAndDeletedFalse(String catererId, Pageable pageable);

    Page<Booking> findByCatererIdAndStatusAndDeletedFalse(
        String catererId, BookingStatus status, Pageable pageable
    );

    // ── Duplicate check: same customer + caterer + date ───────────────
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.customerId = :customerId
        AND b.catererId = :catererId
        AND b.eventDate = :eventDate
        AND b.status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED')
        AND b.deleted = false
    """)
    boolean existsActiveBooking(
        @Param("customerId") String customerId,
        @Param("catererId")  String catererId,
        @Param("eventDate")  LocalDate eventDate
    );

    // ── Caterer availability: any booking on that date ─────────────────
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.catererId = :catererId
        AND b.eventDate = :eventDate
        AND b.status IN ('CONFIRMED', 'IN_PROGRESS')
        AND b.deleted = false
    """)
    boolean isCatererBooked(
        @Param("catererId") String catererId,
        @Param("eventDate") LocalDate eventDate
    );

    // ── Auto-expire: PENDING bookings past their expiry time ──────────
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'PENDING'
        AND b.expiresAt < :now
        AND b.deleted = false
    """)
    List<Booking> findExpiredPendingBookings(@Param("now") LocalDateTime now);

    // ── Upcoming events that need IN_PROGRESS marking ─────────────────
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'CONFIRMED'
        AND b.eventDate = :today
        AND b.deleted = false
    """)
    List<Booking> findConfirmedForToday(@Param("today") LocalDate today);

    // ── Admin: filtered search ─────────────────────────────────────────
    @Query("""
        SELECT b FROM Booking b
        WHERE b.deleted = false
        AND (:status IS NULL OR b.status = :status)
        AND (:occasion IS NULL OR b.occasion = :occasion)
        AND (:city IS NULL OR LOWER(b.eventCity) = LOWER(:city))
        AND (:fromDate IS NULL OR b.eventDate >= :fromDate)
        AND (:toDate IS NULL OR b.eventDate <= :toDate)
        ORDER BY b.createdAt DESC
    """)
    Page<Booking> findFiltered(
        @Param("status")   BookingStatus status,
        @Param("occasion") OccasionType occasion,
        @Param("city")     String city,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate")   LocalDate toDate,
        Pageable pageable
    );

    // ── Statistics ─────────────────────────────────────────────────────
    long countByCatererIdAndStatusAndDeletedFalse(String catererId, BookingStatus status);
    long countByCustomerIdAndDeletedFalse(String customerId);
    long countByStatusAndDeletedFalse(BookingStatus status);

    @Query("""
        SELECT SUM(b.totalAmount) FROM Booking b
        WHERE b.catererId = :catererId
        AND b.status = 'COMPLETED'
        AND b.deleted = false
    """)
    java.math.BigDecimal sumEarningsByCatererId(@Param("catererId") String catererId);

    // ── Status update ──────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Booking b SET b.status = :status, b.updatedAt = :now WHERE b.id = :id")
    void updateStatus(@Param("id") String id,
                      @Param("status") BookingStatus status,
                      @Param("now") LocalDateTime now);
}
