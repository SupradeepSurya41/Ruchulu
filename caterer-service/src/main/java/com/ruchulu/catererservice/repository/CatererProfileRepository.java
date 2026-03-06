package com.ruchulu.catererservice.repository;

import com.ruchulu.catererservice.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CatererProfileRepository extends JpaRepository<CatererProfile, String> {

    // ── Basic lookups ────────────────────────────────────────────────
    Optional<CatererProfile> findByUserIdAndDeletedFalse(String userId);
    Optional<CatererProfile> findByIdAndDeletedFalse(String id);
    boolean existsByUserIdAndDeletedFalse(String userId);
    boolean existsByEmailAndDeletedFalse(String email);

    // ── Status-based listing ─────────────────────────────────────────
    Page<CatererProfile> findByProfileStatusAndDeletedFalse(CatererStatus status, Pageable pageable);
    Page<CatererProfile> findByCityIgnoreCaseAndProfileStatusAndDeletedFalse(
        String city, CatererStatus status, Pageable pageable
    );

    // ── Main Search Query — powers "Find Caterers" button ─────────────
    @Query("""
        SELECT DISTINCT c FROM CatererProfile c
        WHERE c.deleted = false
        AND c.profileStatus = 'ACTIVE'
        AND (:city IS NULL OR LOWER(c.city) = LOWER(:city))
        AND (:guestCount IS NULL OR (c.minGuests <= :guestCount AND c.maxGuests >= :guestCount))
        AND (:vegetarianOnly IS NULL OR :vegetarianOnly = false OR c.isVegetarian = true)
        AND (:fssaiOnly IS NULL OR :fssaiOnly = false OR c.fssaiVerified = true)
        AND (:minRating IS NULL OR c.rating >= :minRating)
        AND (:minBudget IS NULL OR c.pricePerPlateMin >= :minBudget)
        AND (:maxBudget IS NULL OR c.pricePerPlateMax <= :maxBudget)
    """)
    Page<CatererProfile> searchCaterers(
        @Param("city")          String city,
        @Param("guestCount")    Integer guestCount,
        @Param("vegetarianOnly") Boolean vegetarianOnly,
        @Param("fssaiOnly")     Boolean fssaiOnly,
        @Param("minRating")     Double minRating,
        @Param("minBudget")     BigDecimal minBudget,
        @Param("maxBudget")     BigDecimal maxBudget,
        Pageable pageable
    );

    // ── Top-rated caterers by city ───────────────────────────────────
    @Query("""
        SELECT c FROM CatererProfile c
        WHERE c.deleted = false AND c.profileStatus = 'ACTIVE'
        AND LOWER(c.city) = LOWER(:city)
        ORDER BY c.rating DESC, c.totalReviews DESC
    """)
    List<CatererProfile> findTopRatedByCity(@Param("city") String city, Pageable pageable);

    // ── Full text search on name / description ───────────────────────
    @Query("""
        SELECT c FROM CatererProfile c
        WHERE c.deleted = false AND c.profileStatus = 'ACTIVE'
        AND (
            LOWER(c.businessName) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(c.ownerName)    LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(c.description)  LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY c.rating DESC
    """)
    Page<CatererProfile> fullTextSearch(@Param("q") String query, Pageable pageable);

    // ── Rating update ────────────────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE CatererProfile c
        SET c.rating = :rating, c.totalReviews = :totalReviews
        WHERE c.id = :id
    """)
    void updateRating(
        @Param("id")           String id,
        @Param("rating")       Double rating,
        @Param("totalReviews") Integer totalReviews
    );

    // ── Status update ────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE CatererProfile c SET c.profileStatus = :status WHERE c.id = :id")
    void updateStatus(@Param("id") String id, @Param("status") CatererStatus status);

    // ── Stats ────────────────────────────────────────────────────────
    long countByProfileStatusAndDeletedFalse(CatererStatus status);
    long countByCityIgnoreCaseAndDeletedFalse(String city);
}
