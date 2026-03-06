package com.ruchulu.notificationservice.service;

import com.ruchulu.notificationservice.model.NotificationType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EmailTemplateService — Subject & Body Tests")
class EmailTemplateServiceTest {

    private final EmailTemplateService service = new EmailTemplateService();

    @Test @DisplayName("buildSubject() — OTP_LOGIN returns login subject")
    void subject_otpLogin() {
        String s = service.buildSubject(NotificationType.OTP_LOGIN, Map.of());
        assertThat(s).containsIgnoringCase("OTP").containsIgnoringCase("Login");
    }

    @Test @DisplayName("buildSubject() — BOOKING_CONFIRMED contains occasion")
    void subject_bookingConfirmed_containsOccasion() {
        String s = service.buildSubject(NotificationType.BOOKING_CONFIRMED,
            Map.of("occasion", "Wedding"));
        assertThat(s).contains("Wedding");
    }

    @Test @DisplayName("buildSubject() — CATERER_PROFILE_APPROVED returns approval text")
    void subject_catererApproved() {
        String s = service.buildSubject(NotificationType.CATERER_PROFILE_APPROVED, Map.of());
        assertThat(s).containsIgnoringCase("Approved");
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    @DisplayName("buildSubject() — all types return non-null subject")
    void subject_allTypes_nonNull(NotificationType type) {
        assertThat(service.buildSubject(type, Map.of())).isNotNull().isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    @DisplayName("buildBody() — all types produce valid HTML")
    void body_allTypes_validHtml(NotificationType type) {
        String body = service.buildBody(type, Map.of());
        assertThat(body).contains("<!DOCTYPE html>").contains("Ruchulu");
    }

    @Test @DisplayName("buildBody() — OTP body contains OTP code")
    void body_otp_containsCode() {
        String body = service.buildBody(NotificationType.OTP_LOGIN,
            Map.of("name", "Ravi", "otp", "123456", "expiryMinutes", "10"));
        assertThat(body).contains("123456").contains("Ravi").contains("10 minutes");
    }

    @Test @DisplayName("buildBody() — BOOKING_CONFIRMED body contains caterer name")
    void body_bookingConfirmed_containsCaterer() {
        String body = service.buildBody(NotificationType.BOOKING_CONFIRMED,
            Map.of("customerName", "Priya", "catererName", "Good Caterers",
                   "occasion", "Wedding", "eventDate", "2025-12-01",
                   "eventCity", "Hyderabad", "guestCount", "200",
                   "advanceAmount", "10000", "bookingId", "bk-001"));
        assertThat(body).contains("Good Caterers").contains("Priya");
    }

    @Test @DisplayName("buildBody() — WELCOME body contains name")
    void body_welcome_containsName() {
        String body = service.buildBody(NotificationType.ACCOUNT_CREATED,
            Map.of("name", "Lakshmi"));
        assertThat(body).contains("Lakshmi");
    }

    @Test @DisplayName("buildBody() — body is wrapped in layout with header")
    void body_wrappedInLayout() {
        String body = service.buildBody(NotificationType.OTP_LOGIN, Map.of());
        assertThat(body).contains("<body").contains("</body>").contains("Ruchulu 🥄");
    }

    @Test @DisplayName("buildBody() — REVIEW body contains rating and reviewer")
    void body_review_containsRating() {
        String body = service.buildBody(NotificationType.REVIEW_RECEIVED,
            Map.of("catererName", "Lakshmi Caterers", "rating", "5",
                   "reviewerName", "Ravi Kumar", "comment", "Excellent food!"));
        assertThat(body).contains("5").contains("Ravi Kumar").contains("Excellent food!");
    }
}
