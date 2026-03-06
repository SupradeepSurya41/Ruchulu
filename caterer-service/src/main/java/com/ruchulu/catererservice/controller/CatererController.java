package com.ruchulu.catererservice.controller;

import com.ruchulu.catererservice.dto.*;
import com.ruchulu.catererservice.model.*;
import com.ruchulu.catererservice.service.CatererService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CatererController — all caterer profile and search endpoints.
 * Base URL: /api/v1/caterers
 *
 * Frontend → API mapping:
 *   "Find Caterers" button  → GET  /caterers/search
 *   "Register as Caterer"   → POST /caterers/register
 *   "View Profile"          → GET  /caterers/{id}
 *   "My Profile"            → GET  /caterers/me
 *   "Edit Profile"          → PUT  /caterers/me
 */
@RestController
@RequestMapping("/caterers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CatererController {

    private final CatererService catererService;

    // ── PING ─────────────────────────────────────────────────────────────
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("service", "caterer-service", "status", "UP",
            "timestamp", LocalDateTime.now().toString()));
    }

    // ── REGISTER CATERER PROFILE ──────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<CatererProfile> register(
            Authentication auth,
            @Valid @RequestBody CreateCatererRequest request) {
        String userId = (String) auth.getPrincipal();
        CatererProfile saved = catererService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── SEARCH (Find Caterers) ────────────────────────────────────────────
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) Integer guestCount,
            @RequestParam(required = false) Boolean vegetarianOnly,
            @RequestParam(required = false) Boolean fssaiVerifiedOnly,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        CatererSearchRequest searchReq = new CatererSearchRequest();
        searchReq.setCity(city);
        searchReq.setOccasion(occasion != null ? OccasionType.valueOf(occasion.toUpperCase()) : null);
        searchReq.setGuestCount(guestCount);
        searchReq.setVegetarianOnly(vegetarianOnly);
        searchReq.setFssaiVerifiedOnly(fssaiVerifiedOnly);
        searchReq.setMinBudgetPerPlate(minBudget != null ? java.math.BigDecimal.valueOf(minBudget) : null);
        searchReq.setMaxBudgetPerPlate(maxBudget != null ? java.math.BigDecimal.valueOf(maxBudget) : null);
        searchReq.setCuisineType(cuisineType != null ? CuisineType.valueOf(cuisineType.toUpperCase()) : null);
        searchReq.setMinRating(minRating);
        searchReq.setSortBy(sortBy);
        searchReq.setPage(page);
        searchReq.setSize(size);

        Page<CatererProfile> results = catererService.searchCaterers(searchReq);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", results.getContent(),
            "totalElements", results.getTotalElements(),
            "totalPages", results.getTotalPages(),
            "currentPage", results.getNumber()
        ));
    }

    // ── FULL TEXT SEARCH ──────────────────────────────────────────────────
    @GetMapping("/search/text")
    public ResponseEntity<Map<String, Object>> textSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<CatererProfile> results = catererService.fullTextSearch(q, page, size);
        return ResponseEntity.ok(Map.of("success", true, "data", results.getContent(),
            "totalElements", results.getTotalElements()));
    }

    // ── TOP RATED BY CITY ─────────────────────────────────────────────────
    @GetMapping("/top-rated/{city}")
    public ResponseEntity<Map<String, Object>> topRated(
            @PathVariable String city,
            @RequestParam(defaultValue = "6") int limit) {
        List<CatererProfile> results = catererService.getTopRatedByCity(city, limit);
        return ResponseEntity.ok(Map.of("success", true, "data", results));
    }

    // ── GET MY PROFILE ────────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<CatererProfile> getMyProfile(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(catererService.getProfileByUserId(userId));
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────
    @GetMapping("/{catererId}")
    public ResponseEntity<CatererProfile> getById(@PathVariable String catererId) {
        return ResponseEntity.ok(catererService.getProfileById(catererId));
    }

    // ── UPDATE MY PROFILE ─────────────────────────────────────────────────
    @PutMapping("/me")
    public ResponseEntity<CatererProfile> updateMyProfile(
            Authentication auth,
            @Valid @RequestBody UpdateCatererRequest request) {
        String userId = (String) auth.getPrincipal();
        CatererProfile mine = catererService.getProfileByUserId(userId);
        return ResponseEntity.ok(catererService.updateProfile(mine.getId(), userId, request));
    }

    // ── DELETE PROFILE ────────────────────────────────────────────────────
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> deleteMyProfile(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        CatererProfile mine = catererService.getProfileByUserId(userId);
        catererService.deleteProfile(mine.getId(), userId);
        return ResponseEntity.ok(Map.of("success", true,
            "message", "Caterer profile deleted successfully."));
    }

    // ── SUBMIT FSSAI ──────────────────────────────────────────────────────
    @PostMapping("/me/fssai")
    public ResponseEntity<Map<String, Object>> submitFssai(
            Authentication auth,
            @RequestParam String fssaiNumber) {
        String userId = (String) auth.getPrincipal();
        CatererProfile mine = catererService.getProfileByUserId(userId);
        catererService.submitFssai(mine.getId(), userId, fssaiNumber);
        return ResponseEntity.ok(Map.of("success", true,
            "message", "FSSAI number submitted for verification."));
    }
}
