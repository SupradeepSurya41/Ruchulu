package com.ruchulu.catererservice.service;

import com.ruchulu.catererservice.dto.*;
import com.ruchulu.catererservice.exception.*;
import com.ruchulu.catererservice.model.*;
import com.ruchulu.catererservice.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatererService — Unit Tests (Mocked)")
class CatererServiceTest {

    @Mock private CatererProfileRepository catererRepo;
    @Mock private MenuItemRepository       menuRepo;
    @Mock private CatererReviewRepository  reviewRepo;

    @InjectMocks private CatererServiceImpl service;

    // ── CREATE PROFILE ─────────────────────────────────────────────────────
    @Test @DisplayName("createProfile() — success creates and returns profile")
    void createProfile_success() {
        when(catererRepo.existsByUserIdAndDeletedFalse("usr-001")).thenReturn(false);
        when(catererRepo.save(any())).thenAnswer(i -> {
            CatererProfile p = i.getArgument(0);
            p.setId("cat-001");
            return p;
        });

        CatererProfile result = service.createProfile("usr-001", buildCreateRequest());

        assertThat(result.getId()).isEqualTo("cat-001");
        assertThat(result.getBusinessName()).isEqualTo("Good Caterers");
        assertThat(result.getProfileStatus()).isEqualTo(CatererStatus.PENDING_APPROVAL);
        verify(catererRepo).save(any());
    }

    @Test @DisplayName("createProfile() — duplicate throws CatererAlreadyExistsException")
    void createProfile_duplicate_throws() {
        when(catererRepo.existsByUserIdAndDeletedFalse("usr-001")).thenReturn(true);
        assertThatThrownBy(() -> service.createProfile("usr-001", buildCreateRequest()))
            .isInstanceOf(CatererAlreadyExistsException.class);
    }

    @Test @DisplayName("createProfile() — unsupported city throws CityNotSupportedException")
    void createProfile_badCity_throws() {
        when(catererRepo.existsByUserIdAndDeletedFalse("usr-001")).thenReturn(false);
        CreateCatererRequest req = buildCreateRequest();
        req.setCity("Mumbai");
        assertThatThrownBy(() -> service.createProfile("usr-001", req))
            .isInstanceOf(CityNotSupportedException.class);
    }

    // ── GET PROFILE ────────────────────────────────────────────────────────
    @Test @DisplayName("getProfileById() — found returns profile")
    void getProfileById_found() {
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(activeProfile()));
        assertThat(service.getProfileById("cat-001").getBusinessName()).isEqualTo("Good Caterers");
    }

    @Test @DisplayName("getProfileById() — not found throws CatererNotFoundException")
    void getProfileById_notFound_throws() {
        when(catererRepo.findByIdAndDeletedFalse("cat-xxx")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProfileById("cat-xxx"))
            .isInstanceOf(CatererNotFoundException.class);
    }

    // ── UPDATE PROFILE ─────────────────────────────────────────────────────
    @Test @DisplayName("updateProfile() — success updates fields")
    void updateProfile_success() {
        CatererProfile p = activeProfile();
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(p));
        when(catererRepo.save(any())).thenReturn(p);

        UpdateCatererRequest req = new UpdateCatererRequest();
        req.setBusinessName("Updated Caterers");
        service.updateProfile("cat-001", "usr-001", req);

        assertThat(p.getBusinessName()).isEqualTo("Updated Caterers");
    }

    @Test @DisplayName("updateProfile() — wrong owner throws UnauthorizedCatererAccessException")
    void updateProfile_wrongOwner_throws() {
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(activeProfile()));
        assertThatThrownBy(() ->
            service.updateProfile("cat-001", "usr-WRONG", new UpdateCatererRequest()))
            .isInstanceOf(UnauthorizedCatererAccessException.class);
    }

    // ── DELETE PROFILE ─────────────────────────────────────────────────────
    @Test @DisplayName("deleteProfile() — marks as deleted")
    void deleteProfile_success() {
        CatererProfile p = activeProfile();
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(p));
        when(catererRepo.save(any())).thenReturn(p);

        service.deleteProfile("cat-001", "usr-001");
        assertThat(p.getDeleted()).isTrue();
    }

    // ── ADMIN OPERATIONS ───────────────────────────────────────────────────
    @Test @DisplayName("approveProfile() — sets status to ACTIVE")
    void approveProfile_setsActive() {
        CatererProfile p = activeProfile();
        p.setProfileStatus(CatererStatus.PENDING_APPROVAL);
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(p));
        when(catererRepo.save(any())).thenReturn(p);

        service.approveProfile("cat-001");
        assertThat(p.getProfileStatus()).isEqualTo(CatererStatus.ACTIVE);
    }

    @Test @DisplayName("rejectProfile() — sets status to REJECTED with reason")
    void rejectProfile_setsRejected() {
        CatererProfile p = activeProfile();
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(p));
        when(catererRepo.save(any())).thenReturn(p);

        service.rejectProfile("cat-001", "Missing FSSAI");
        assertThat(p.getProfileStatus()).isEqualTo(CatererStatus.REJECTED);
        assertThat(p.getRejectionReason()).isEqualTo("Missing FSSAI");
    }

    // ── FSSAI ──────────────────────────────────────────────────────────────
    @Test @DisplayName("submitFssai() — invalid number throws InvalidFssaiNumberException")
    void submitFssai_invalid_throws() {
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(activeProfile()));
        assertThatThrownBy(() -> service.submitFssai("cat-001", "usr-001", "INVALID"))
            .isInstanceOf(InvalidFssaiNumberException.class);
    }

    @Test @DisplayName("submitFssai() — valid 14-digit number succeeds")
    void submitFssai_valid_succeeds() {
        CatererProfile p = activeProfile();
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(p));
        when(catererRepo.save(any())).thenReturn(p);

        assertThatNoException().isThrownBy(() ->
            service.submitFssai("cat-001", "usr-001", "12345678901234"));
        assertThat(p.getFssaiNumber()).isEqualTo("12345678901234");
    }

    // ── REVIEWS ────────────────────────────────────────────────────────────
    @Test @DisplayName("addReview() — duplicate booking throws ReviewAlreadyExistsException")
    void addReview_duplicate_throws() {
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(activeProfile()));
        when(reviewRepo.existsByBookingId("bk-001")).thenReturn(true);

        CreateReviewRequest req = new CreateReviewRequest("bk-001", 4, "Great!", 5, 4, 4);
        assertThatThrownBy(() ->
            service.addReview("cat-001", "usr-002", "Priya Kumar", req))
            .isInstanceOf(ReviewAlreadyExistsException.class);
    }

    @Test @DisplayName("addReview() — success saves review and recalculates rating")
    void addReview_success() {
        CatererProfile p = activeProfile();
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(p));
        when(reviewRepo.existsByBookingId("bk-002")).thenReturn(false);
        when(reviewRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(reviewRepo.findAverageRatingByCatererId("cat-001")).thenReturn(Optional.of(4.5));
        when(reviewRepo.countByCatererId("cat-001")).thenReturn(5L);

        CreateReviewRequest req = new CreateReviewRequest("bk-002", 5, "Excellent food!", 5, 5, 5);
        CatererReview review = service.addReview("cat-001", "usr-002", "Priya", req);

        assertThat(review.getRating()).isEqualTo(5);
        verify(catererRepo).updateRating(eq("cat-001"), anyDouble(), anyInt());
    }

    // ── MENU ITEMS ─────────────────────────────────────────────────────────
    @Test @DisplayName("addMenuItem() — wrong owner throws UnauthorizedCatererAccessException")
    void addMenuItem_wrongOwner_throws() {
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(activeProfile()));

        MenuItemRequest req = new MenuItemRequest("Biryani", "Dum biryani",
            MenuCategory.RICE_AND_BIRYANI, CuisineType.HYDERABADI,
            false, BigDecimal.valueOf(180), null, true, false);

        assertThatThrownBy(() ->
            service.addMenuItem("cat-001", "usr-WRONG", req))
            .isInstanceOf(UnauthorizedCatererAccessException.class);
    }

    @Test @DisplayName("deleteMenuItem() — not found throws MenuItemNotFoundException")
    void deleteMenuItem_notFound_throws() {
        when(catererRepo.findByIdAndDeletedFalse("cat-001")).thenReturn(Optional.of(activeProfile()));
        when(menuRepo.findByIdAndCaterer_Id("menu-xxx", "cat-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.deleteMenuItem("cat-001", "menu-xxx", "usr-001"))
            .isInstanceOf(MenuItemNotFoundException.class);
    }

    // ── TEST DATA ─────────────────────────────────────────────────────────
    private CatererProfile activeProfile() {
        return CatererProfile.builder()
                .id("cat-001").userId("usr-001")
                .businessName("Good Caterers").ownerName("Ravi")
                .email("ravi@gmail.com").phone("9876543210")
                .city("Hyderabad").address("Banjara Hills")
                .minGuests(50).maxGuests(500)
                .pricePerPlateMin(BigDecimal.valueOf(250))
                .pricePerPlateMax(BigDecimal.valueOf(600))
                .profileStatus(CatererStatus.ACTIVE)
                .fssaiVerified(true).deleted(false)
                .occasions(Set.of(OccasionType.WEDDING))
                .build();
    }

    private CreateCatererRequest buildCreateRequest() {
        return CreateCatererRequest.builder()
                .businessName("Good Caterers").ownerName("Ravi Kumar")
                .email("ravi@gmail.com").phone("9876543210")
                .city("Hyderabad").address("Banjara Hills, Hyderabad")
                .isVegetarian(true).isNonVegetarian(true)
                .minGuests(50).maxGuests(500)
                .pricePerPlateMin(BigDecimal.valueOf(250))
                .pricePerPlateMax(BigDecimal.valueOf(600))
                .experienceYears(5)
                .occasions(Set.of(OccasionType.WEDDING, OccasionType.BIRTHDAY))
                .cuisineTypes(Set.of(CuisineType.ANDHRA, CuisineType.HYDERABADI))
                .build();
    }
}
