package com.ruchulu.notificationservice.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service @Slf4j
class JwtService {
    @Value("${app.jwt.secret}") private String secret;
    public String extractUserId(String t) { return claims(t).getSubject(); }
    public String extractRole(String t)   { return claims(t).get("role", String.class); }
    public boolean isValid(String t) {
        try { claims(t); return true; } catch (JwtException e) { return false; }
    }
    private Claims claims(String t) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(t).getPayload();
    }
    private SecretKey key() {
        byte[] k = secret.getBytes(StandardCharsets.UTF_8), p = new byte[32];
        System.arraycopy(k, 0, p, 0, Math.min(k.length, 32));
        return Keys.hmacShaKeyFor(p);
    }
}

@Component @RequiredArgsConstructor @Slf4j
class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            try {
                if (jwt.isValid(token)) {
                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(jwt.extractUserId(token), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + jwt.extractRole(token)))));
                }
            } catch (Exception e) { log.warn("JWT filter: {}", e.getMessage()); }
        }
        chain.doFilter(req, res);
    }
}

@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {
    @Value("${app.cors.allowed-origins}") private List<String> origins;
    private final JwtAuthFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(cors()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/notifications/ping", "/notifications/send",
                    "/notifications/send/otp", "/notifications/send/booking",
                    "/h2-console/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource cors() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
