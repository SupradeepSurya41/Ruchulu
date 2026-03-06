package com.ruchulu.catererservice.model;

import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CatererProfile Model — Field & Behaviour Tests")
class CatererProfileModelTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            validator = f.getValidator();
        }
    }

    // ── Defaults ───────────────────────────────────────────────────────────
    @Test @DisplayName("Default status is PENDING_APPROVAL")
    void defaultStatus() {
        assertThat(CatererProfile.builder().build().getProfileStatus())
            .isEqualTo(CatererStatus.PENDING_APPROVAL);
    }

    @Test @DisplayName("Default isVegetarian is true")
    void defaultVegetarian() {
        assertThat(CatererProfile.builder().build().getIsVegetarian()).isTrue();
    }

    @Test @DisplayName("Default deleted is false")
    void defaultDeleted() {
        assertThat(CatererProfile.builder().build().getDeleted()).isFalse();
    }

    @Test @DisplayName("Default minGuests is 50")
    void defaultMinGuests() {
        assertThat(CatererProfile.builder().build().getMinGuests()).isEqualTo(50);
    }

    @Test @DisplayName("Default rating is 0.0")
    void defaultRating() {
        assertThat(CatererProfile.builder().build().getRating()).isEqualTo(0.0);
    }

    // ── businessName validation ────────────────────────────────────────────
    @Test @DisplayName("businessName — null fails")
    void businessName_null_fails() {
        CatererProfile p = validProfile(); p.setBusinessName(null);
        assertViolationOn(p, "businessName");
    }

    @Test @DisplayName("businessName — 2 chars fails (min=3)")
    void businessName_tooShort_fails() {
        CatererProfile p = validProfile(); p.setBusinessName("AB");
        assertViolationOn(p, "businessName");
    }

    @Test @DisplayName("businessName — 121 chars fails (max=120)")
    void businessName_tooLong_fails() {
        CatererProfile p = validProfile(); p.setBusinessName("A".repeat(121));
        assertViolationOn(p, "businessName");
    }

    // ── phone validation ───────────────────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "8765432109"})
    @DisplayName("phone — valid Indian numbers pass")
    void phone_valid(String phone) {
        CatererProfile p = validProfile(); p.setPhone(phone);
        assertNoViolationOn(p, "phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "5000000000"})
    @DisplayName("phone — invalid numbers fail")
    void phone_invalid(String phone) {
        CatererProfile p = validProfile(); p.setPhone(phone);
        assertViolationOn(p, "phone");
    }

    // ── minGuests ──────────────────────────────────────────────────────────
    @Test @DisplayName("minGuests — 9 fails (min=10)")
    void minGuests_belowMin_fails() {
        CatererProfile p = validProfile(); p.setMinGuests(9);
        assertViolationOn(p, "minGuests");
    }

    @Test @DisplayName("maxGuests — 10001 fails (max=10000)")
    void maxGuests_aboveMax_fails() {
        CatererProfile p = validProfile(); p.setMaxGuests(10001);
        assertViolationOn(p, "maxGuests");
    }

    // ── Domain helpers ────────────────────────────────────────────────────
    @Test @DisplayName("isActive() — ACTIVE + deleted=false → true")
    void isActive_true() {
        CatererProfile p = validProfile();
        p.setProfileStatus(CatererStatus.ACTIVE);
        assertThat(p.isActive()).isTrue();
    }

    @Test @DisplayName("isActive() — PENDING_APPROVAL → false")
    void isActive_pending_false() {
        CatererProfile p = validProfile();
        p.setProfileStatus(CatererStatus.PENDING_APPROVAL);
        assertThat(p.isActive()).isFalse();
    }

    @Test @DisplayName("canServe() — within range → true")
    void canServe_withinRange_true() {
        CatererProfile p = validProfile();
        p.setMinGuests(50); p.setMaxGuests(500);
        assertThat(p.canServe(200)).isTrue();
    }

    @Test @DisplayName("canServe() — below min → false")
    void canServe_belowMin_false() {
        CatererProfile p = validProfile();
        p.setMinGuests(100); p.setMaxGuests(500);
        assertThat(p.canServe(50)).isFalse();
    }

    @Test @DisplayName("servesOccasion() — occasion in set → true")
    void servesOccasion_true() {
        CatererProfile p = validProfile();
        p.setOccasions(Set.of(OccasionType.WEDDING, OccasionType.BIRTHDAY));
        assertThat(p.servesOccasion(OccasionType.WEDDING)).isTrue();
    }

    @Test @DisplayName("servesOccasion() — occasion not in set → false")
    void servesOccasion_false() {
        CatererProfile p = validProfile();
        p.setOccasions(Set.of(OccasionType.BIRTHDAY));
        assertThat(p.servesOccasion(OccasionType.WEDDING)).isFalse();
    }

    @Test @DisplayName("markDeleted() — sets deleted=true + DEACTIVATED")
    void markDeleted_setsFields() {
        CatererProfile p = validProfile();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        p.markDeleted();
        assertThat(p.getDeleted()).isTrue();
        assertThat(p.getProfileStatus()).isEqualTo(CatererStatus.DEACTIVATED);
        assertThat(p.getDeletedAt()).isAfter(before);
    }

    @Test @DisplayName("updateRating() — rounds to 2 decimals")
    void updateRating_rounds() {
        CatererProfile p = validProfile();
        p.updateRating(4.6666666, 15);
        assertThat(p.getRating()).isEqualTo(4.67);
        assertThat(p.getTotalReviews()).isEqualTo(15);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private CatererProfile validProfile() {
        return CatererProfile.builder()
                .userId("usr-001").businessName("Good Caterers").ownerName("Ravi Kumar")
                .email("ravi@gmail.com").phone("9876543210")
                .city("Hyderabad").address("1-2-3 Banjara Hills, Hyderabad")
                .minGuests(50).maxGuests(500)
                .pricePerPlateMin(BigDecimal.valueOf(200))
                .pricePerPlateMax(BigDecimal.valueOf(600))
                .profileStatus(CatererStatus.ACTIVE).deleted(false)
                .build();
    }

    private void assertViolationOn(CatererProfile p, String field) {
        assertThat(validator.validate(p)).anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    private void assertNoViolationOn(CatererProfile p, String field) {
        assertThat(validator.validate(p)).noneMatch(v -> v.getPropertyPath().toString().equals(field));
    }
}
