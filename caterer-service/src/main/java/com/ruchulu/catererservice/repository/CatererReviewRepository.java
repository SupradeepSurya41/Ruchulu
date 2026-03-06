package com.ruchulu.catererservice.repository;

import com.ruchulu.catererservice.model.CatererReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatererReviewRepository extends JpaRepository<CatererReview, String> {

    Page<CatererReview> findByCaterer_Id(String catererId, Pageable pageable);
    Page<CatererReview> findByCaterer_IdAndIsVerifiedTrue(String catererId, Pageable pageable);
    boolean existsByBookingId(String bookingId);
    Optional<CatererReview> findByBookingId(String bookingId);

    @Query("SELECT AVG(r.rating) FROM CatererReview r WHERE r.caterer.id = :catererId")
    Optional<Double> findAverageRatingByCatererId(@Param("catererId") String catererId);

    @Query("SELECT COUNT(r) FROM CatererReview r WHERE r.caterer.id = :catererId")
    long countByCatererId(@Param("catererId") String catererId);

    @Query("""
        SELECT AVG(r.foodRating) FROM CatererReview r
        WHERE r.caterer.id = :catererId AND r.foodRating IS NOT NULL
    """)
    Optional<Double> findAverageFoodRating(@Param("catererId") String catererId);
}
