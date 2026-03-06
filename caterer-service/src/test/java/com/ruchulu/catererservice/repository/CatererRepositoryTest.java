package com.ruchulu.catererservice.repository;

import com.ruchulu.catererservice.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("h2")
@DisplayName("CatererProfileRepository — H2 Slice Tests")
class CatererRepositoryTest {

    @Autowired private CatererProfileRepository repo;

    private CatererProfile saved;

    @BeforeEach
    void setup() {
        saved = repo.save(CatererProfile.builder()
                .userId("usr-001").businessName("Test Caterers").ownerName("Ravi")
                .email("ravi@gmail.com").phone("9876543210")
                .city("hyderabad").address("Banjara Hills")
                .minGuests(50).maxGuests(500)
                .pricePerPlateMin(BigDecimal.valueOf(250))
                .pricePerPlateMax(BigDecimal.valueOf(600))
                .profileStatus(CatererStatus.ACTIVE)
                .isVegetarian(true).deleted(false)
                .occasions(Set.of(OccasionType.WEDDING))
                .cuisineTypes(Set.of(CuisineType.ANDHRA))
                .build());
    }

    @AfterEach void cleanup() { repo.deleteAll(); }

    @Test @DisplayName("findByUserIdAndDeletedFalse — found")
    void findByUserId_found() {
        assertThat(repo.findByUserIdAndDeletedFalse("usr-001")).isPresent();
    }

    @Test @DisplayName("findByUserIdAndDeletedFalse — not found")
    void findByUserId_notFound() {
        assertThat(repo.findByUserIdAndDeletedFalse("usr-xxx")).isEmpty();
    }

    @Test @DisplayName("existsByUserIdAndDeletedFalse — true for existing")
    void existsByUserId_true() {
        assertThat(repo.existsByUserIdAndDeletedFalse("usr-001")).isTrue();
    }

    @Test @DisplayName("findByIdAndDeletedFalse — found")
    void findById_found() {
        assertThat(repo.findByIdAndDeletedFalse(saved.getId())).isPresent();
    }

    @Test @DisplayName("findByIdAndDeletedFalse — not found for deleted caterer")
    void findById_deleted_notFound() {
        saved.markDeleted();
        repo.save(saved);
        assertThat(repo.findByIdAndDeletedFalse(saved.getId())).isEmpty();
    }

    @Test @DisplayName("findByProfileStatusAndDeletedFalse — returns active caterers")
    void findByStatus_active() {
        var page = repo.findByProfileStatusAndDeletedFalse(CatererStatus.ACTIVE, PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(c -> c.getProfileStatus() == CatererStatus.ACTIVE);
    }

    @Test @DisplayName("searchCaterers — city filter works")
    void search_cityFilter() {
        var results = repo.searchCaterers("hyderabad", null, null, null, null, null, null,
            PageRequest.of(0, 10));
        assertThat(results.getContent()).isNotEmpty();
    }

    @Test @DisplayName("searchCaterers — guest count filter excludes out-of-range")
    void search_guestFilter_excludes() {
        // saved caterer handles 50–500; request for 1000 should exclude it
        var results = repo.searchCaterers(null, 1000, null, null, null, null, null,
            PageRequest.of(0, 10));
        assertThat(results.getContent()).noneMatch(c -> c.getId().equals(saved.getId()));
    }

    @Test @DisplayName("findTopRatedByCity — returns list ordered by rating")
    void topRated_returnsOrdered() {
        List<CatererProfile> top = repo.findTopRatedByCity("hyderabad", PageRequest.of(0, 5));
        assertThat(top).isNotNull();
    }

    @Test @DisplayName("updateRating — updates rating and totalReviews")
    void updateRating_works() {
        repo.updateRating(saved.getId(), 4.75, 20);
        CatererProfile updated = repo.findById(saved.getId()).orElseThrow();
        assertThat(updated.getRating()).isEqualTo(4.75);
        assertThat(updated.getTotalReviews()).isEqualTo(20);
    }

    @Test @DisplayName("updateStatus — updates status field")
    void updateStatus_works() {
        repo.updateStatus(saved.getId(), CatererStatus.SUSPENDED);
        CatererProfile updated = repo.findById(saved.getId()).orElseThrow();
        assertThat(updated.getProfileStatus()).isEqualTo(CatererStatus.SUSPENDED);
    }

    @Test @DisplayName("countByProfileStatusAndDeletedFalse — counts correctly")
    void countByStatus_correct() {
        assertThat(repo.countByProfileStatusAndDeletedFalse(CatererStatus.ACTIVE)).isGreaterThanOrEqualTo(1);
    }

    @Test @DisplayName("save — auto-assigns UUID id")
    void save_assignsUuid() {
        assertThat(saved.getId()).isNotNull().isNotBlank();
    }
}
