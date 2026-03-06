package com.ruchulu.catererservice.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service @Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public String extractUserId(String token) { return parseClaims(token).getSubject(); }
    public String extractRole(String token)   { return parseClaims(token).get("role", String.class); }

    public boolean isTokenValid(String token) {
        try { parseClaims(token); return true; }
        catch (JwtException e) { log.warn("JWT invalid: {}", e.getMessage()); return false; }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    private SecretKey signingKey() {
        byte[] k = jwtSecret.getBytes(StandardCharsets.UTF_8);
        byte[] padded = new byte[32];
        System.arraycopy(k, 0, padded, 0, Math.min(k.length, 32));
        return Keys.hmacShaKeyFor(padded);
    }
}
