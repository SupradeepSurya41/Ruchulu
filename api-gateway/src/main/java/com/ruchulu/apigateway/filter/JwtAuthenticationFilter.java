package com.ruchulu.apigateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * JwtAuthenticationFilter — Spring Cloud Gateway filter.
 * Validates Bearer JWT on protected routes before forwarding to downstream services.
 * Adds X-User-Id and X-User-Role headers so downstream services know the caller.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Extract Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Gateway: missing/invalid Authorization header for path={}", path);
                return unauthorized(exchange, "Missing or invalid Authorization header.");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = validateToken(token);
                String userId = claims.getSubject();
                String role   = claims.get("role", String.class);

                log.debug("Gateway: JWT valid — userId={}, role={}, path={}", userId, role, path);

                // Forward enriched headers to downstream service
                ServerHttpRequest enriched = request.mutate()
                        .header("X-User-Id",   userId)
                        .header("X-User-Role", role != null ? role : "")
                        .build();

                return chain.filter(exchange.mutate().request(enriched).build());

            } catch (ExpiredJwtException e) {
                log.warn("Gateway: expired JWT for path={}", path);
                return unauthorized(exchange, "Token has expired. Please log in again.");
            } catch (JwtException e) {
                log.warn("Gateway: invalid JWT for path={} — {}", path, e.getMessage());
                return unauthorized(exchange, "Invalid token.");
            }
        };
    }

    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        byte[] padded   = new byte[32];
        System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
        return Keys.hmacShaKeyFor(padded);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
            {"success":false,"errorCode":"UNAUTHORIZED","message":"%s","timestamp":"%s"}
            """.formatted(message, LocalDateTime.now());

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // No config needed for now
    }
}
