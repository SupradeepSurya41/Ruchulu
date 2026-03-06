package com.ruchulu.catererservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CatererProfile — core entity representing a caterer's business listing.
 * A user with role=CATERER has exactly one CatererProfile.
 * Maps to the "Find Caterers" section of the Ruchulu frontend.
 */
@Entity
@Table(
    name = "caterer_profiles",
    indexes = {
        @Index(name = "idx_caterer_city",   columnList = "city"),
        @Index(name = "idx_caterer_status", columnList = "profile_status"),
        @Index(name = "idx_caterer_rating", columnList = "rating"),
        @Index(name = "idx_caterer_user",   columnList = "user_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"menuItems", "reviews"})
public class CatererProfile {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    // ── Linked to User Service ────────────────────────────────────────────
    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    // ── Business Info ─────────────────────────────────────────────────────
    @NotBlank(message = "Business name is required")
    @Size(min = 3, max = 120, message = "Business name must be 3–120 characters")
    @Column(name = "business_name", nullable = false, length = 120)
    private String businessName;

    @NotBlank(message = "Owner name is required")
    @Size(min = 2, max = 80)
    @Column(name = "owner_name", nullable = false, length = 80)
    private String ownerName;

    @NotBlank(message = "Email is required")
    @Email
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @NotBlank(message = "City is required")
    @Column(name = "city", nullable = false, length = 60)
    private String city;

    @NotBlank(message = "Address is required")
    @Size(max = 300)
    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "caterer_gallery", joinColumns = @JoinColumn(name = "caterer_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    private List<String> galleryUrls = new ArrayList<>();

    // ── FSSAI Verification ───────────────────────────────────────────────
    @Column(name = "fssai_number", length = 20)
    private String fssaiNumber;

    @Column(name = "fssai_verified", nullable = false)
    @Builder.Default
    private Boolean fssaiVerified = false;

    @Column(name = "fssai_expiry_date")
    private java.time.LocalDate fssaiExpiryDate;

    // ── Food Type Flags ───────────────────────────────────────────────────
    @Column(name = "is_vegetarian", nullable = false)
    @Builder.Default
    private Boolean isVegetarian = true;

    @Column(name = "is_non_vegetarian", nullable = false)
    @Builder.Default
    private Boolean isNonVegetarian = false;

    @Column(name = "is_vegan", nullable = false)
    @Builder.Default
    private Boolean isVegan = false;

    // ── Capacity & Pricing ────────────────────────────────────────────────
    @Min(value = 10, message = "Minimum guests must be at least 10")
    @Column(name = "min_guests", nullable = false)
    @Builder.Default
    private Integer minGuests = 50;

    @Max(value = 10000, message = "Maximum guests cannot exceed 10,000")
    @Column(name = "max_guests", nullable = false)
    @Builder.Default
    private Integer maxGuests = 500;

    @DecimalMin(value = "100.0", message = "Minimum price per plate must be ₹100")
    @Column(name = "price_per_plate_min", precision = 10, scale = 2)
    private BigDecimal pricePerPlateMin;

    @DecimalMax(value = "5000.0", message = "Maximum price per plate cannot exceed ₹5000")
    @Column(name = "price_per_plate_max", precision = 10, scale = 2)
    private BigDecimal pricePerPlateMax;

    @Min(0) @Max(50)
    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 0;

    // ── Occasions served ─────────────────────────────────────────────────
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "caterer_occasions", joinColumns = @JoinColumn(name = "caterer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "occasion", length = 30)
    @Builder.Default
    private Set<OccasionType> occasions = new HashSet<>();

    // ── Cuisine Types served ──────────────────────────────────────────────
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "caterer_cuisines", joinColumns = @JoinColumn(name = "caterer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "cuisine", length = 30)
    @Builder.Default
    private Set<CuisineType> cuisineTypes = new HashSet<>();

    // ── Menu & Reviews (relationships) ────────────────────────────────────
    @OneToMany(mappedBy = "caterer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuItem> menuItems = new ArrayList<>();

    @OneToMany(mappedBy = "caterer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CatererReview> reviews = new ArrayList<>();

    // ── Ratings (computed & cached) ───────────────────────────────────────
    @Column(name = "rating", precision = 3, scale = 2)
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    // ── Availability ──────────────────────────────────────────────────────
    @Column(name = "advance_booking_days", nullable = false)
    @Builder.Default
    private Integer advanceBookingDays = 3;  // needs 3 days notice

    // ── Status ────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false, length = 20)
    @Builder.Default
    private CatererStatus profileStatus = CatererStatus.PENDING_APPROVAL;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    // ── Timestamps & soft delete ──────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Domain helpers ────────────────────────────────────────────────────
    public boolean isActive() {
        return CatererStatus.ACTIVE.equals(this.profileStatus) && !Boolean.TRUE.equals(this.deleted);
    }

    public boolean canServe(int guestCount) {
        return guestCount >= this.minGuests && guestCount <= this.maxGuests;
    }

    public boolean servesOccasion(OccasionType occasion) {
        return this.occasions.contains(occasion);
    }

    public void updateRating(double newRating, int newTotalReviews) {
        this.rating       = Math.round(newRating * 100.0) / 100.0;
        this.totalReviews = newTotalReviews;
    }

    public void markDeleted() {
        this.deleted       = true;
        this.deletedAt     = LocalDateTime.now();
        this.profileStatus = CatererStatus.DEACTIVATED;
    }
}
