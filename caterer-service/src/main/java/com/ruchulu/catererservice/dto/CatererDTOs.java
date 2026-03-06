package com.ruchulu.catererservice.dto;

import com.ruchulu.catererservice.model.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

// ═══════════════════════════════════════════════════════════════
// CREATE / REGISTER CATERER PROFILE REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCatererRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 3, max = 120)
    private String businessName;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String phone;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Address is required")
    private String address;

    @Size(max = 1000)
    private String description;

    // FSSAI
    @Pattern(regexp = "^[0-9]{14}$", message = "FSSAI number must be exactly 14 digits")
    private String fssaiNumber;

    // Food type
    private Boolean isVegetarian;
    private Boolean isNonVegetarian;
    private Boolean isVegan;

    // Capacity
    @Min(10) private Integer minGuests;
    @Max(10000) private Integer maxGuests;

    @DecimalMin("100.0") private BigDecimal pricePerPlateMin;
    @DecimalMax("5000.0") private BigDecimal pricePerPlateMax;

    @Min(0) @Max(50)
    private Integer experienceYears;

    private Set<OccasionType> occasions;
    private Set<CuisineType>  cuisineTypes;
}

// ═══════════════════════════════════════════════════════════════
// UPDATE CATERER PROFILE REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class UpdateCatererRequest {
    @Size(min = 3, max = 120) private String businessName;
    private String description;
    private String address;
    @Pattern(regexp = "^[6-9]\\d{9}$") private String phone;
    private Boolean isVegetarian;
    private Boolean isNonVegetarian;
    private Boolean isVegan;
    @Min(10) private Integer minGuests;
    @Max(10000) private Integer maxGuests;
    @DecimalMin("100.0") private BigDecimal pricePerPlateMin;
    @DecimalMax("5000.0") private BigDecimal pricePerPlateMax;
    @Min(0) @Max(50) private Integer experienceYears;
    private Set<OccasionType> occasions;
    private Set<CuisineType>  cuisineTypes;
    private Integer advanceBookingDays;
    private String profilePictureUrl;
}

// ═══════════════════════════════════════════════════════════════
// CATERER PROFILE RESPONSE (full detail)
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CatererProfileResponse {
    private String          id;
    private String          userId;
    private String          businessName;
    private String          ownerName;
    private String          email;
    private String          phone;
    private String          city;
    private String          address;
    private String          description;
    private String          profilePictureUrl;
    private List<String>    galleryUrls;
    private String          fssaiNumber;
    private Boolean         fssaiVerified;
    private Boolean         isVegetarian;
    private Boolean         isNonVegetarian;
    private Boolean         isVegan;
    private Integer         minGuests;
    private Integer         maxGuests;
    private BigDecimal      pricePerPlateMin;
    private BigDecimal      pricePerPlateMax;
    private Integer         experienceYears;
    private Double          rating;
    private Integer         totalReviews;
    private Set<OccasionType> occasions;
    private Set<CuisineType>  cuisineTypes;
    private CatererStatus   profileStatus;
    private Integer         advanceBookingDays;
    private LocalDateTime   createdAt;
    private List<MenuItemResponse> menuItems;
}

// ═══════════════════════════════════════════════════════════════
// CATERER SUMMARY (used in listing / search results)
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CatererSummaryResponse {
    private String        id;
    private String        businessName;
    private String        ownerName;
    private String        city;
    private String        profilePictureUrl;
    private Boolean       fssaiVerified;
    private Boolean       isVegetarian;
    private Boolean       isNonVegetarian;
    private Integer       minGuests;
    private Integer       maxGuests;
    private BigDecimal    pricePerPlateMin;
    private BigDecimal    pricePerPlateMax;
    private Double        rating;
    private Integer       totalReviews;
    private Integer       experienceYears;
    private Set<OccasionType> occasions;
    private Set<CuisineType>  cuisineTypes;
}

// ═══════════════════════════════════════════════════════════════
// CATERER SEARCH / FILTER REQUEST
// Maps to "Find Caterers" filters on frontend
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class CatererSearchRequest {
    private String        city;
    private OccasionType  occasion;
    private Integer       guestCount;
    private Boolean       vegetarianOnly;
    private Boolean       fssaiVerifiedOnly;
    @DecimalMin("0") private BigDecimal maxBudgetPerPlate;
    @DecimalMin("0") private BigDecimal minBudgetPerPlate;
    private CuisineType   cuisineType;
    @Min(0) @Max(5) private Double minRating;
    private String        sortBy;       // "rating", "price_asc", "price_desc", "experience"
    @Min(0) private Integer page;
    @Min(1) @Max(50) private Integer size;
}

// ═══════════════════════════════════════════════════════════════
// MENU ITEM CREATE/UPDATE REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class MenuItemRequest {
    @NotBlank(message = "Item name is required") @Size(min = 2, max = 100)
    private String name;
    @Size(max = 500) private String description;
    @NotNull(message = "Category is required") private MenuCategory category;
    private CuisineType cuisineType;
    private Boolean isVegetarian;
    @DecimalMin("10.0") @DecimalMax("5000.0") private BigDecimal pricePerPlate;
    private String  imageUrl;
    private Boolean isAvailable;
    private Boolean isSeasonal;
}

// ═══════════════════════════════════════════════════════════════
// MENU ITEM RESPONSE
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class MenuItemResponse {
    private String      id;
    private String      name;
    private String      description;
    private MenuCategory category;
    private CuisineType cuisineType;
    private Boolean     isVegetarian;
    private BigDecimal  pricePerPlate;
    private String      imageUrl;
    private Boolean     isAvailable;
    private Boolean     isSeasonal;
}

// ═══════════════════════════════════════════════════════════════
// REVIEW CREATE REQUEST
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class CreateReviewRequest {
    @NotBlank private String bookingId;
    @Min(1) @Max(5) @NotNull private Integer rating;
    @Size(min = 10, max = 1000) private String comment;
    @Min(1) @Max(5) private Integer foodRating;
    @Min(1) @Max(5) private Integer serviceRating;
    @Min(1) @Max(5) private Integer valueRating;
}

// ═══════════════════════════════════════════════════════════════
// REVIEW RESPONSE
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class ReviewResponse {
    private String    id;
    private String    userId;
    private String    userName;
    private Integer   rating;
    private String    comment;
    private Integer   foodRating;
    private Integer   serviceRating;
    private Integer   valueRating;
    private Boolean   isVerified;
    private String    catererResponse;
    private LocalDateTime createdAt;
}

// ═══════════════════════════════════════════════════════════════
// FSSAI VERIFY REQUEST (admin action)
// ═══════════════════════════════════════════════════════════════
@Data @NoArgsConstructor @AllArgsConstructor
class FssaiVerifyRequest {
    @NotBlank private String catererId;
    @NotNull  private Boolean approved;
    private String rejectionReason;
}

// ═══════════════════════════════════════════════════════════════
// GENERIC API RESPONSE WRAPPER
// ═══════════════════════════════════════════════════════════════
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class ApiResponse<T> {
    private boolean       success;
    private String        message;
    private T             data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return ApiResponse.<T>builder().success(true).message(msg).data(data)
                .timestamp(LocalDateTime.now()).build();
    }
    public static <T> ApiResponse<T> ok(String msg) { return ok(msg, null); }
    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.<T>builder().success(false).message(msg)
                .timestamp(LocalDateTime.now()).build();
    }
}
