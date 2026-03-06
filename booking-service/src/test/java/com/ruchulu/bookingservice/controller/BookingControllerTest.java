package com.ruchulu.bookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ruchulu.bookingservice.dto.*;
import com.ruchulu.bookingservice.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@DisplayName("BookingController — Integration Tests (H2)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingControllerTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void configMapper() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ── PING ──────────────────────────────────────────────────────────────
    @Test @Order(1) @DisplayName("GET /bookings/ping — returns UP")
    void ping_returnsUp() throws Exception {
        mockMvc.perform(get("/bookings/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("booking-service"));
    }

    // ── SEED DATA TESTS ───────────────────────────────────────────────────
    @Test @Order(2) @DisplayName("GET /bookings/bk-001 — without auth returns 403")
    void getBooking_noAuth_403() throws Exception {
        mockMvc.perform(get("/bookings/bk-001"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(3) @DisplayName("POST /bookings — without auth returns 403")
    void createBooking_noAuth_403() throws Exception {
        CreateBookingRequest req = buildCreateRequest();
        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test @Order(4) @DisplayName("GET /bookings/my — without auth returns 403")
    void myBookings_noAuth_403() throws Exception {
        mockMvc.perform(get("/bookings/my"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(5) @DisplayName("POST /bookings/{id}/confirm — without auth returns 403")
    void confirm_noAuth_403() throws Exception {
        mockMvc.perform(post("/bookings/bk-001/confirm"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(6) @DisplayName("POST /bookings/{id}/cancel — missing reason returns 403 (no auth)")
    void cancel_noAuth_403() throws Exception {
        mockMvc.perform(post("/bookings/bk-001/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Changed plans\"}"))
                .andExpect(status().isForbidden());
    }

    // ── VALIDATION ────────────────────────────────────────────────────────
    @Test @Order(7) @DisplayName("POST /bookings — missing catererId returns 4xx")
    void createBooking_missingCatererId_4xx() throws Exception {
        CreateBookingRequest req = buildCreateRequest();
        req.setCatererId(null);
        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test @Order(8) @DisplayName("POST /bookings — missing occasion returns 4xx")
    void createBooking_missingOccasion_4xx() throws Exception {
        CreateBookingRequest req = buildCreateRequest();
        req.setOccasion(null);
        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test @Order(9) @DisplayName("POST /bookings — past event date returns 4xx")
    void createBooking_pastDate_4xx() throws Exception {
        CreateBookingRequest req = buildCreateRequest();
        req.setEventDate(LocalDate.now().minusDays(1));
        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test @Order(10) @DisplayName("POST /bookings — guestCount 0 returns 4xx")
    void createBooking_zeroGuests_4xx() throws Exception {
        CreateBookingRequest req = buildCreateRequest();
        req.setGuestCount(0);
        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test @Order(11) @DisplayName("POST /bookings — guestCount 10001 returns 4xx")
    void createBooking_tooManyGuests_4xx() throws Exception {
        CreateBookingRequest req = buildCreateRequest();
        req.setGuestCount(10001);
        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test @Order(12) @DisplayName("GET /bookings/caterer/incoming — without auth returns 403")
    void catererIncoming_noAuth_403() throws Exception {
        mockMvc.perform(get("/bookings/caterer/incoming"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(13) @DisplayName("GET /admin/bookings — without auth returns 403")
    void adminBookings_noAuth_403() throws Exception {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isForbidden());
    }

    // ── UNKNOWN BOOKING ───────────────────────────────────────────────────
    @Test @Order(14) @DisplayName("GET /bookings/{id}/timeline — without auth returns 403")
    void timeline_noAuth_403() throws Exception {
        mockMvc.perform(get("/bookings/bk-001/timeline"))
                .andExpect(status().isForbidden());
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private CreateBookingRequest buildCreateRequest() {
        return CreateBookingRequest.builder()
                .catererId("cat-001")
                .occasion(OccasionType.WEDDING)
                .eventDate(LocalDate.now().plusDays(10))
                .eventCity("Hyderabad")
                .eventAddress("12-3-456, Banjara Hills, Hyderabad")
                .guestCount(200)
                .specialRequests("No pork")
                .build();
    }
}
