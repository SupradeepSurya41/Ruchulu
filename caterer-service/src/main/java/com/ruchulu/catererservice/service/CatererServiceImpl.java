package com.ruchulu.catererservice.service;

import com.ruchulu.catererservice.dto.*;
import com.ruchulu.catererservice.exception.*;
import com.ruchulu.catererservice.model.*;
import com.ruchulu.catererservice.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CatererServiceImpl implements CatererService {

    private final CatererProfileRepository catererRepo;
    private final MenuItemRepository       menuRepo;
    private final CatererReviewRepository  reviewRepo;

    private static final Set<String> SUPPORTED_CITIES = Set.of(
        "hyderabad","secunderabad","warangal","vijayawada","visakhapatnam",
        "guntur","tirupati","karimnagar","nellore","nizamabad","kurnool","rajahmundry"
    );

    // ── CREATE PROFILE ────────────────────────────────────────────────────
    @Override
    public CatererProfile createProfile(String userId, CreateCatererRequest req) {
        if (catererRepo.existsByUserIdAndDeletedFalse(userId)) {
            throw new CatererAlreadyExistsException(userId);
        }
        validateCity(req.getCity());

        CatererProfile profile = CatererProfile.builder()
                .userId(userId)
                .businessName(req.getBusinessName().trim())
                .ownerName(req.getOwnerName().trim())
                .email(req.getEmail().toLowerCase().trim())
                .phone(req.getPhone())
                .city(normalizeCity(req.getCity()))
                .address(req.getAddress())
                .description(req.getDescription())
                .fssaiNumber(req.getFssaiNumber())
                .isVegetarian(req.getIsVegetarian() != null ? req.getIsVegetarian() : true)
                .isNonVegetarian(req.getIsNonVegetarian() != null ? req.getIsNonVegetarian() : false)
                .isVegan(req.getIsVegan() != null ? req.getIsVegan() : false)
                .minGuests(req.getMinGuests() != null ? req.getMinGuests() : 50)
                .maxGuests(req.getMaxGuests() != null ? req.getMaxGuests() : 500)
                .pricePerPlateMin(req.getPricePerPlateMin())
                .pricePerPlateMax(req.getPricePerPlateMax())
                .experienceYears(req.getExperienceYears() != null ? req.getExperienceYears() : 0)
                .occasions(req.getOccasions() != null ? req.getOccasions() : Set.of())
                .cuisineTypes(req.getCuisineTypes() != null ? req.getCuisineTypes() : Set.of())
                .profileStatus(CatererStatus.PENDING_APPROVAL)
                .build();

        CatererProfile saved = catererRepo.save(profile);
        log.info("Caterer profile created: id={}, user={}", saved.getId(), userId);
        return saved;
    }

    // ── GET PROFILE ───────────────────────────────────────────────────────
    @Override
    public CatererProfile getProfileById(String id) {
        return catererRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CatererNotFoundException(id));
    }

    @Override
    public CatererProfile getProfileByUserId(String userId) {
        return catererRepo.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CatererNotFoundException("userId=" + userId));
    }

    // ── UPDATE PROFILE ────────────────────────────────────────────────────
    @Override
    public CatererProfile updateProfile(String catererId, String requestingUserId, UpdateCatererRequest req) {
        CatererProfile p = getProfileById(catererId);
        guardOwnership(p, requestingUserId);

        if (req.getBusinessName()      != null) p.setBusinessName(req.getBusinessName().trim());
        if (req.getDescription()       != null) p.setDescription(req.getDescription());
        if (req.getAddress()           != null) p.setAddress(req.getAddress());
        if (req.getPhone()             != null) p.setPhone(req.getPhone());
        if (req.getIsVegetarian()      != null) p.setIsVegetarian(req.getIsVegetarian());
        if (req.getIsNonVegetarian()   != null) p.setIsNonVegetarian(req.getIsNonVegetarian());
        if (req.getIsVegan()           != null) p.setIsVegan(req.getIsVegan());
        if (req.getMinGuests()         != null) p.setMinGuests(req.getMinGuests());
        if (req.getMaxGuests()         != null) p.setMaxGuests(req.getMaxGuests());
        if (req.getPricePerPlateMin()  != null) p.setPricePerPlateMin(req.getPricePerPlateMin());
        if (req.getPricePerPlateMax()  != null) p.setPricePerPlateMax(req.getPricePerPlateMax());
        if (req.getExperienceYears()   != null) p.setExperienceYears(req.getExperienceYears());
        if (req.getOccasions()         != null) p.setOccasions(req.getOccasions());
        if (req.getCuisineTypes()      != null) p.setCuisineTypes(req.getCuisineTypes());
        if (req.getAdvanceBookingDays()!= null) p.setAdvanceBookingDays(req.getAdvanceBookingDays());
        if (req.getProfilePictureUrl() != null) p.setProfilePictureUrl(req.getProfilePictureUrl());

        return catererRepo.save(p);
    }

    // ── DELETE PROFILE ────────────────────────────────────────────────────
    @Override
    public void deleteProfile(String catererId, String requestingUserId) {
        CatererProfile p = getProfileById(catererId);
        guardOwnership(p, requestingUserId);
        p.markDeleted();
        catererRepo.save(p);
        log.info("Caterer profile soft-deleted: id={}", catererId);
    }

    // ── SEARCH (powers "Find Caterers" button) ────────────────────────────
    @Override
    public Page<CatererProfile> searchCaterers(CatererSearchRequest req) {
        int page = req.getPage() != null ? req.getPage() : 0;
        int size = req.getSize() != null ? req.getSize() : 12;

        Sort sort = buildSort(req.getSortBy());
        Pageable pageable = PageRequest.of(page, size, sort);

        return catererRepo.searchCaterers(
            req.getCity() != null ? req.getCity().toLowerCase() : null,
            req.getGuestCount(),
            req.getVegetarianOnly(),
            req.getFssaiVerifiedOnly(),
            req.getMinRating(),
            req.getMinBudgetPerPlate(),
            req.getMaxBudgetPerPlate(),
            pageable
        );
    }

    @Override
    public List<CatererProfile> getTopRatedByCity(String city, int limit) {
        return catererRepo.findTopRatedByCity(city, PageRequest.of(0, limit));
    }

    @Override
    public Page<CatererProfile> fullTextSearch(String query, int page, int size) {
        return catererRepo.fullTextSearch(query, PageRequest.of(page, size));
    }

    // ── ADMIN OPERATIONS ──────────────────────────────────────────────────
    @Override
    public void approveProfile(String catererId) {
        CatererProfile p = getProfileById(catererId);
        p.setProfileStatus(CatererStatus.ACTIVE);
        p.setRejectionReason(null);
        catererRepo.save(p);
        log.info("Caterer approved: id={}", catererId);
    }

    @Override
    public void rejectProfile(String catererId, String reason) {
        CatererProfile p = getProfileById(catererId);
        p.setProfileStatus(CatererStatus.REJECTED);
        p.setRejectionReason(reason);
        catererRepo.save(p);
        log.info("Caterer rejected: id={}, reason={}", catererId, reason);
    }

    @Override
    public void suspendCaterer(String catererId, String reason) {
        CatererProfile p = getProfileById(catererId);
        p.setProfileStatus(CatererStatus.SUSPENDED);
        p.setRejectionReason(reason);
        catererRepo.save(p);
        log.info("Caterer suspended: id={}", catererId);
    }

    @Override
    public Page<CatererProfile> getPendingApprovals(int page, int size) {
        return catererRepo.findByProfileStatusAndDeletedFalse(
            CatererStatus.PENDING_APPROVAL, PageRequest.of(page, size)
        );
    }

    // ── FSSAI ─────────────────────────────────────────────────────────────
    @Override
    public void submitFssai(String catererId, String userId, String fssaiNumber) {
        if (!fssaiNumber.matches("^[0-9]{14}$")) {
            throw new InvalidFssaiNumberException(fssaiNumber);
        }
        CatererProfile p = getProfileById(catererId);
        guardOwnership(p, userId);
        p.setFssaiNumber(fssaiNumber);
        catererRepo.save(p);
    }

    @Override
    public void verifyFssai(String catererId, boolean approved, String reason) {
        CatererProfile p = getProfileById(catererId);
        p.setFssaiVerified(approved);
        if (!approved) p.setRejectionReason(reason);
        catererRepo.save(p);
        log.info("FSSAI verification for caterer {}: approved={}", catererId, approved);
    }

    // ── MENU ITEMS ────────────────────────────────────────────────────────
    @Override
    public MenuItem addMenuItem(String catererId, String userId, MenuItemRequest req) {
        CatererProfile p = getProfileById(catererId);
        guardOwnership(p, userId);

        MenuItem item = MenuItem.builder()
                .caterer(p)
                .name(req.getName().trim())
                .description(req.getDescription())
                .category(req.getCategory())
                .cuisineType(req.getCuisineType())
                .isVegetarian(req.getIsVegetarian() != null ? req.getIsVegetarian() : true)
                .pricePerPlate(req.getPricePerPlate())
                .imageUrl(req.getImageUrl())
                .isAvailable(req.getIsAvailable() != null ? req.getIsAvailable() : true)
                .isSeasonal(req.getIsSeasonal() != null ? req.getIsSeasonal() : false)
                .build();

        return menuRepo.save(item);
    }

    @Override
    public MenuItem updateMenuItem(String catererId, String menuItemId, String userId, MenuItemRequest req) {
        CatererProfile p = getProfileById(catererId);
        guardOwnership(p, userId);

        MenuItem item = menuRepo.findByIdAndCaterer_Id(menuItemId, catererId)
                .orElseThrow(() -> new MenuItemNotFoundException(menuItemId));

        if (req.getName()        != null) item.setName(req.getName().trim());
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getCategory()    != null) item.setCategory(req.getCategory());
        if (req.getCuisineType() != null) item.setCuisineType(req.getCuisineType());
        if (req.getIsVegetarian()!= null) item.setIsVegetarian(req.getIsVegetarian());
        if (req.getPricePerPlate()!=null) item.setPricePerPlate(req.getPricePerPlate());
        if (req.getImageUrl()    != null) item.setImageUrl(req.getImageUrl());
        if (req.getIsAvailable() != null) item.setIsAvailable(req.getIsAvailable());
        if (req.getIsSeasonal()  != null) item.setIsSeasonal(req.getIsSeasonal());

        return menuRepo.save(item);
    }

    @Override
    public void deleteMenuItem(String catererId, String menuItemId, String userId) {
        CatererProfile p = getProfileById(catererId);
        guardOwnership(p, userId);
        MenuItem item = menuRepo.findByIdAndCaterer_Id(menuItemId, catererId)
                .orElseThrow(() -> new MenuItemNotFoundException(menuItemId));
        menuRepo.delete(item);
    }

    @Override
    public List<MenuItem> getMenuItems(String catererId) {
        getProfileById(catererId); // validate caterer exists
        return menuRepo.findByCaterer_IdAndIsAvailableTrue(catererId);
    }

    // ── REVIEWS ───────────────────────────────────────────────────────────
    @Override
    public CatererReview addReview(String catererId, String userId, String userName, CreateReviewRequest req) {
        CatererProfile p = getProfileById(catererId);

        if (reviewRepo.existsByBookingId(req.getBookingId())) {
            throw new ReviewAlreadyExistsException(req.getBookingId());
        }

        CatererReview review = CatererReview.builder()
                .caterer(p)
                .userId(userId)
                .userName(userName)
                .bookingId(req.getBookingId())
                .rating(req.getRating())
                .comment(req.getComment())
                .foodRating(req.getFoodRating())
                .serviceRating(req.getServiceRating())
                .valueRating(req.getValueRating())
                .isVerified(true)
                .build();

        CatererReview saved = reviewRepo.save(review);

        // Recalculate and cache rating on caterer profile
        recalculateRating(catererId);

        return saved;
    }

    @Override
    public Page<CatererReview> getReviews(String catererId, int page, int size) {
        getProfileById(catererId);
        return reviewRepo.findByCaterer_Id(catererId,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Override
    public void addCatererResponse(String reviewId, String catererId, String response) {
        CatererReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new MenuItemNotFoundException(reviewId));
        review.setCatererResponse(response);
        review.setCatererRespondedAt(LocalDateTime.now());
        reviewRepo.save(review);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private void recalculateRating(String catererId) {
        double avg    = reviewRepo.findAverageRatingByCatererId(catererId).orElse(0.0);
        long   total  = reviewRepo.countByCatererId(catererId);
        catererRepo.updateRating(catererId, Math.round(avg * 100.0) / 100.0, (int) total);
    }

    private void guardOwnership(CatererProfile profile, String requestingUserId) {
        if (!profile.getUserId().equals(requestingUserId)) {
            throw new UnauthorizedCatererAccessException();
        }
    }

    private void validateCity(String city) {
        if (city != null && !SUPPORTED_CITIES.contains(city.toLowerCase())) {
            throw new CityNotSupportedException(city);
        }
    }

    private String normalizeCity(String city) {
        if (city == null) return null;
        String lower = city.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by("rating").descending();
        return switch (sortBy.toLowerCase()) {
            case "price_asc"   -> Sort.by("pricePerPlateMin").ascending();
            case "price_desc"  -> Sort.by("pricePerPlateMax").descending();
            case "experience"  -> Sort.by("experienceYears").descending();
            case "newest"      -> Sort.by("createdAt").descending();
            default            -> Sort.by("rating").descending();
        };
    }
}
