package com.ruchulu.catererservice.service;

import com.ruchulu.catererservice.dto.*;
import com.ruchulu.catererservice.model.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CatererService {

    // Profile CRUD
    CatererProfile createProfile(String userId, CreateCatererRequest request);
    CatererProfile getProfileById(String catererId);
    CatererProfile getProfileByUserId(String userId);
    CatererProfile updateProfile(String catererId, String requestingUserId, UpdateCatererRequest request);
    void deleteProfile(String catererId, String requestingUserId);

    // Search — powers "Find Caterers" button on frontend
    Page<CatererProfile> searchCaterers(CatererSearchRequest request);
    List<CatererProfile> getTopRatedByCity(String city, int limit);
    Page<CatererProfile> fullTextSearch(String query, int page, int size);

    // Admin operations
    void approveProfile(String catererId);
    void rejectProfile(String catererId, String reason);
    void suspendCaterer(String catererId, String reason);
    Page<CatererProfile> getPendingApprovals(int page, int size);

    // FSSAI
    void submitFssai(String catererId, String userId, String fssaiNumber);
    void verifyFssai(String catererId, boolean approved, String reason);

    // Menu
    MenuItem addMenuItem(String catererId, String userId, MenuItemRequest request);
    MenuItem updateMenuItem(String catererId, String menuItemId, String userId, MenuItemRequest request);
    void deleteMenuItem(String catererId, String menuItemId, String userId);
    List<MenuItem> getMenuItems(String catererId);

    // Reviews
    CatererReview addReview(String catererId, String userId, String userName, CreateReviewRequest request);
    Page<CatererReview> getReviews(String catererId, int page, int size);
    void addCatererResponse(String reviewId, String catererId, String response);
}
