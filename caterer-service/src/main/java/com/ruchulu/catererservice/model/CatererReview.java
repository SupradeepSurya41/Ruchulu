package com.ruchulu.catererservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * CatererReview — a review left by a customer after a booking.
 * Rating recalculated and cached in CatererProfile on every save.
 */
@Entity
@Table(
    name = "caterer_reviews",
    indexes = {
        @Index(name = "idx_review_caterer", columnList = "caterer_id"),
        @Index(name = "idx_review_user",    columnList = "user_id"),
        @Index(name = "idx_review_booking", columnList = "booking_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_booking", columnNames = "booking_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CatererReview {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caterer_id", nullable = false)
    private CatererProfile caterer;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "booking_id", nullable = false, length = 36)
    private String bookingId;

    @Min(1) @Max(5)
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Size(min = 10, max = 1000, message = "Review must be 10–1000 characters")
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "food_rating")
    private Integer foodRating;       // 1–5 sub-rating

    @Column(name = "service_rating")
    private Integer serviceRating;    // 1–5 sub-rating

    @Column(name = "value_rating")
    private Integer valueRating;      // 1–5 sub-rating

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;  // true = from a completed booking

    @Column(name = "caterer_response", columnDefinition = "TEXT")
    private String catererResponse;

    @Column(name = "caterer_responded_at")
    private LocalDateTime catererRespondedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
