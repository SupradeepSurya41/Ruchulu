package com.ruchulu.catererservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MenuItem — an individual dish offered by a caterer.
 * Multiple menu items belong to one CatererProfile.
 */
@Entity
@Table(
    name = "menu_items",
    indexes = {
        @Index(name = "idx_menu_caterer",   columnList = "caterer_id"),
        @Index(name = "idx_menu_category",  columnList = "category"),
        @Index(name = "idx_menu_available", columnList = "is_available")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class MenuItem {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caterer_id", nullable = false)
    private CatererProfile caterer;

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 100, message = "Item name must be 2–100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private MenuCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "cuisine_type", length = 30)
    private CuisineType cuisineType;

    @Column(name = "is_vegetarian", nullable = false)
    @Builder.Default
    private Boolean isVegetarian = true;

    @DecimalMin(value = "10.0", message = "Price must be at least ₹10")
    @DecimalMax(value = "5000.0", message = "Price cannot exceed ₹5000")
    @Column(name = "price_per_plate", precision = 10, scale = 2)
    private BigDecimal pricePerPlate;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "is_seasonal", nullable = false)
    @Builder.Default
    private Boolean isSeasonal = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
