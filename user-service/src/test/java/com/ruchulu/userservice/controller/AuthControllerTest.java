package com.ruchulu.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruchulu.userservice.dto.LoginRequest;
import com.ruchulu.userservice.dto.RegisterRequest;
import com.ruchulu.userservice.model.UserRole;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@DisplayName("AuthController — Integration Tests (H2)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;

    private static final String BASE = "/auth";

    // ── PING ─────────────────────────────────────────────────────────────
    @Test @Order(1) @DisplayName("GET /auth/ping — returns 200 UP")
    void ping_returns200() throws Exception {
        mockMvc.perform(get(BASE + "/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ── REGISTER ─────────────────────────────────────────────────────────
    @Test @Order(2) @DisplayName("POST /auth/register — valid request returns 201")
    void register_valid_201() throws Exception {
        performRegister("newuser@gmail.com", "9111111110")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("newuser@gmail.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test @Order(3) @DisplayName("POST /auth/register — duplicate email returns 409")
    void register_duplicateEmail_409() throws Exception {
        performRegister("dup@gmail.com", "9222222220").andExpect(status().isCreated());
        performRegister("dup@gmail.com", "9333333330")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_EMAIL_DUPLICATE"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test @Order(4) @DisplayName("POST /auth/register — missing firstName returns 400")
    void register_missingFirstName_400() throws Exception {
        RegisterRequest req = buildReq("req@gmail.com", "9444444440");
        req.setFirstName(null);
        performPost(BASE + "/register", req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test @Order(5) @DisplayName("POST /auth/register — disposable email returns 400")
    void register_disposableEmail_400() throws Exception {
        RegisterRequest req = buildReq("user@mailinator.com", "9555555550");
        performPost(BASE + "/register", req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test @Order(6) @DisplayName("POST /auth/register — invalid phone returns 400")
    void register_invalidPhone_400() throws Exception {
        RegisterRequest req = buildReq("phone@gmail.com", "1234567890"); // starts with 1
        performPost(BASE + "/register", req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test @Order(7) @DisplayName("POST /auth/register — weak password returns 400")
    void register_weakPassword_400() throws Exception {
        RegisterRequest req = buildReq("weak@gmail.com", "9666666660");
        req.setPassword("weakpassword"); // no uppercase, digit, special
        performPost(BASE + "/register", req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test @Order(8) @DisplayName("POST /auth/register — unsupported city returns 400")
    void register_badCity_400() throws Exception {
        RegisterRequest req = buildReq("city@gmail.com", "9777777770");
        req.setCity("Mumbai"); // not in AP/TS
        performPost(BASE + "/register", req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.city").exists());
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────
    @Test @Order(9) @DisplayName("POST /auth/login — valid creds returns 200 + otpRequired=true")
    void login_valid_200_otpRequired() throws Exception {
        performRegister("login@gmail.com", "9888888880").andExpect(status().isCreated());

        LoginRequest req = new LoginRequest("login@gmail.com", "Secure@1234");
        performPost(BASE + "/login", req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpRequired").value(true))
                .andExpect(jsonPath("$.message").value(containsString("OTP sent")));
    }

    @Test @Order(10) @DisplayName("POST /auth/login — wrong password returns 401")
    void login_wrongPwd_401() throws Exception {
        performRegister("wrongpwd@gmail.com", "9999999990").andExpect(status().isCreated());

        LoginRequest req = new LoginRequest("wrongpwd@gmail.com", "WrongPass@1");
        performPost(BASE + "/login", req)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test @Order(11) @DisplayName("POST /auth/login — unknown user returns 401")
    void login_unknownUser_401() throws Exception {
        LoginRequest req = new LoginRequest("nobody@gmail.com", "Secure@1234");
        performPost(BASE + "/login", req)
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(12) @DisplayName("POST /auth/login — blank identifier returns 400")
    void login_blankIdentifier_400() throws Exception {
        LoginRequest req = new LoginRequest("", "Secure@1234");
        performPost(BASE + "/login", req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.identifier").exists());
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────
    @Test @Order(13) @DisplayName("POST /auth/forgot-password — valid email returns 200")
    void forgotPassword_200() throws Exception {
        performRegister("forgot@gmail.com", "9100000001").andExpect(status().isCreated());

        performPost(BASE + "/forgot-password",
            java.util.Map.of("email", "forgot@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private ResultActions performRegister(String email, String phone) throws Exception {
        return performPost(BASE + "/register", buildReq(email, phone));
    }

    private ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private RegisterRequest buildReq(String email, String phone) {
        return RegisterRequest.builder()
                .firstName("Ravi").lastName("Kumar")
                .email(email).phone(phone)
                .password("Secure@1234").city("Hyderabad")
                .role(UserRole.CUSTOMER).build();
    }
}
