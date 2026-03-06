package com.ruchulu.catererservice.controller;

import com.ruchulu.catererservice.dto.*;
import com.ruchulu.catererservice.model.*;
import com.ruchulu.catererservice.service.CatererService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MenuController — CRUD for menu items under a caterer.
 * Base: /api/v1/caterers/{catererId}/menu
 */
@RestController
@RequestMapping("/caterers/{catererId}/menu")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
class MenuController {

    private final CatererService catererService;

    @GetMapping
    public ResponseEntity<List<MenuItem>> getMenu(@PathVariable String catererId) {
        return ResponseEntity.ok(catererService.getMenuItems(catererId));
    }

    @PostMapping
    public ResponseEntity<MenuItem> addItem(
            @PathVariable String catererId,
            Authentication auth,
            @Valid @RequestBody MenuItemRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catererService.addMenuItem(catererId, userId, request));
    }

    @PutMapping("/{menuItemId}")
    public ResponseEntity<MenuItem> updateItem(
            @PathVariable String catererId,
            @PathVariable String menuItemId,
            Authentication auth,
            @Valid @RequestBody MenuItemRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(catererService.updateMenuItem(catererId, menuItemId, userId, request));
    }

    @DeleteMapping("/{menuItemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(
            @PathVariable String catererId,
            @PathVariable String menuItemId,
            Authentication auth) {
        String userId = (String) auth.getPrincipal();
        catererService.deleteMenuItem(catererId, menuItemId, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Menu item deleted."));
    }
}

/**
 * ReviewController — reviews for a caterer.
 * Base: /api/v1/caterers/{catererId}/reviews
 */
@RestController
@RequestMapping("/caterers/{catererId}/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
class ReviewController {

    private final CatererService catererService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getReviews(
            @PathVariable String catererId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CatererReview> reviews = catererService.getReviews(catererId, page, size);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", reviews.getContent(),
            "totalElements", reviews.getTotalElements(),
            "totalPages", reviews.getTotalPages()
        ));
    }

    @PostMapping
    public ResponseEntity<CatererReview> addReview(
            @PathVariable String catererId,
            Authentication auth,
            @Valid @RequestBody CreateReviewRequest request) {
        String userId   = (String) auth.getPrincipal();
        String userName = "User"; // in real flow, fetch from user-service
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catererService.addReview(catererId, userId, userName, request));
    }

    @PostMapping("/{reviewId}/respond")
    public ResponseEntity<Map<String, Object>> respond(
            @PathVariable String catererId,
            @PathVariable String reviewId,
            Authentication auth,
            @RequestParam String response) {
        catererService.addCatererResponse(reviewId, catererId, response);
        return ResponseEntity.ok(Map.of("success", true, "message", "Response added."));
    }
}

/**
 * AdminCatererController — admin-only caterer management.
 * Base: /api/v1/admin/caterers
 */
@RestController
@RequestMapping("/admin/caterers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
class AdminCatererController {

    private final CatererService catererService;

    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CatererProfile> results = catererService.getPendingApprovals(page, size);
        return ResponseEntity.ok(Map.of("success", true, "data", results.getContent(),
            "totalElements", results.getTotalElements()));
    }

    @PostMapping("/{catererId}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String catererId) {
        catererService.approveProfile(catererId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Caterer approved."));
    }

    @PostMapping("/{catererId}/reject")
    public ResponseEntity<Map<String, Object>> reject(
            @PathVariable String catererId,
            @RequestParam String reason) {
        catererService.rejectProfile(catererId, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Caterer rejected."));
    }

    @PostMapping("/{catererId}/suspend")
    public ResponseEntity<Map<String, Object>> suspend(
            @PathVariable String catererId,
            @RequestParam String reason) {
        catererService.suspendCaterer(catererId, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Caterer suspended."));
    }

    @PostMapping("/{catererId}/fssai/verify")
    public ResponseEntity<Map<String, Object>> verifyFssai(
            @PathVariable String catererId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason) {
        catererService.verifyFssai(catererId, approved, reason);
        return ResponseEntity.ok(Map.of("success", true,
            "message", approved ? "FSSAI verified." : "FSSAI rejected."));
    }
}
