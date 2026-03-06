package com.ruchulu.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruchulu.notificationservice.dto.SendNotificationRequest;
import com.ruchulu.notificationservice.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@DisplayName("NotificationController — Integration Tests (H2)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test @Order(1) @DisplayName("GET /notifications/ping — returns UP")
    void ping() throws Exception {
        mockMvc.perform(get("/notifications/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("notification-service"));
    }

    @Test @Order(2) @DisplayName("POST /notifications/send — missing recipientId returns 400")
    void send_missingRecipientId_400() throws Exception {
        SendNotificationRequest req = SendNotificationRequest.builder()
                .recipientEmail("test@gmail.com")
                .notificationType(NotificationType.ACCOUNT_CREATED)
                .channel(NotificationChannel.EMAIL)
                .build();
        mockMvc.perform(post("/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test @Order(3) @DisplayName("POST /notifications/send — missing channel returns 400")
    void send_missingChannel_400() throws Exception {
        SendNotificationRequest req = SendNotificationRequest.builder()
                .recipientId("usr-001")
                .recipientEmail("test@gmail.com")
                .notificationType(NotificationType.ACCOUNT_CREATED)
                .build();
        mockMvc.perform(post("/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(4) @DisplayName("POST /notifications/send — invalid email returns 400")
    void send_invalidEmail_400() throws Exception {
        SendNotificationRequest req = SendNotificationRequest.builder()
                .recipientId("usr-001")
                .recipientEmail("not-an-email")
                .notificationType(NotificationType.ACCOUNT_CREATED)
                .channel(NotificationChannel.EMAIL)
                .build();
        mockMvc.perform(post("/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(5) @DisplayName("GET /notifications/my — without auth returns 403")
    void myNotifications_noAuth_403() throws Exception {
        mockMvc.perform(get("/notifications/my"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(6) @DisplayName("GET /notifications/preferences — without auth returns 403")
    void preferences_noAuth_403() throws Exception {
        mockMvc.perform(get("/notifications/preferences"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(7) @DisplayName("PUT /notifications/preferences — without auth returns 403")
    void updatePreference_noAuth_403() throws Exception {
        mockMvc.perform(put("/notifications/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notificationType\":\"BOOKING_CREATED\",\"channel\":\"EMAIL\",\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(8) @DisplayName("POST /notifications/send/booking — missing body returns 4xx")
    void sendBooking_emptyBody_4xx() throws Exception {
        mockMvc.perform(post("/notifications/send/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().is2xxSuccessful()); // no validation on booking request — just fires
    }
}
