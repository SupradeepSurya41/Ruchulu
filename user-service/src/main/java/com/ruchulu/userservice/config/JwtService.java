package com.ruchulu.userservice.config;

import com.ruchulu.userservice.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    // ── Generate ─────────────────────────────────────────────────────
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role",      user.getRole().name());
        claims.put("email",     user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("type",      "ACCESS");
        return buildToken(claims, user.getId(), accessTokenExpiryMs);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "REFRESH");
        return buildToken(claims, user.getId(), refreshTokenExpiryMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiryMs) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    // ── Validate & Extract ────────────────────────────────────────────
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired");
        } catch (JwtException e) {
            log.warn("JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Pad/truncate to 32 bytes for HS256
        byte[] keyPadded = new byte[32];
        System.arraycopy(keyBytes, 0, keyPadded, 0, Math.min(keyBytes.length, 32));
        return Keys.hmacShaKeyFor(keyPadded);
    }
}
