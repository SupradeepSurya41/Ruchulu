package com.ruchulu.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API Gateway — JWT Filter & Route Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwtAuthenticationFilterTest {

    @Autowired
    private WebTestClient webClient;

    private static final String SECRET =
        "cnVjaHVsdS1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmctZm9yLWhzMjU2";

    // ── Ping — public endpoint, no JWT needed ──────────────────────────────
    @Test @Order(1) @DisplayName("Gateway is up — actuator health returns 200")
    void actuatorHealth_up() {
        webClient.get().uri("/actuator/health")
                .exchange()
                .andExpect(res -> Assertions.assertEquals(200, res.getRawStatusCode()));
    }

    // ── Missing token ──────────────────────────────────────────────────────
    @Test @Order(2) @DisplayName("Protected route without token returns 401")
    void noToken_protected_401() {
        webClient.get().uri("/api/v1/users/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test @Order(3) @DisplayName("Protected route with Bearer prefix missing returns 401")
    void malformedToken_401() {
        webClient.get().uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── Expired token ──────────────────────────────────────────────────────
    @Test @Order(4) @DisplayName("Expired JWT returns 401 with expired message")
    void expiredToken_401() {
        String expired = buildToken("usr-001", "CUSTOMER", -3600000L); // expired 1h ago
        webClient.get().uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").value(msg ->
                    Assertions.assertTrue(msg.toString().toLowerCase().contains("expired")));
    }

    // ── Invalid token ──────────────────────────────────────────────────────
    @Test @Order(5) @DisplayName("Tampered JWT returns 401")
    void tamperedToken_401() {
        webClient.get().uri("/api/v1/bookings/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiJ9.tampered.signature")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── Valid token — downstream unavailable (expected 5xx or 401 for bad route) ──
    @Test @Order(6) @DisplayName("Valid JWT passes gateway filter (downstream may be down)")
    void validToken_passesFilter() {
        String token = buildToken("usr-001", "CUSTOMER", 3600000L); // valid for 1h
        webClient.get().uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().value(status ->
                    Assertions.assertTrue(status != 401,
                        "JWT filter should pass valid token — got 401 unexpectedly"));
    }

    @Test @Order(7) @DisplayName("Admin JWT passes gateway filter for admin routes")
    void adminToken_passesFilter() {
        String token = buildToken("usr-admin", "ADMIN", 3600000L);
        webClient.get().uri("/api/v1/admin/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().value(status ->
                    Assertions.assertTrue(status != 401));
    }

    // ── Public routes — no token needed ───────────────────────────────────
    @Test @Order(8) @DisplayName("Public caterer search route passes without token")
    void publicCatererSearch_noTokenNeeded() {
        // Gateway lets through; downstream may or may not be running
        webClient.get().uri("/api/v1/caterers/search")
                .exchange()
                .expectStatus().value(status ->
                    Assertions.assertTrue(status != 401,
                        "Public route should not require JWT"));
    }

    // ── Helper: build test JWT ─────────────────────────────────────────────
    private String buildToken(String userId, String role, long expiryOffset) {
        byte[] k = SECRET.getBytes(StandardCharsets.UTF_8);
        byte[] p = new byte[32];
        System.arraycopy(k, 0, p, 0, Math.min(k.length, 32));
        SecretKey key = Keys.hmacShaKeyFor(p);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryOffset))
                .signWith(key)
                .compact();
    }
}
