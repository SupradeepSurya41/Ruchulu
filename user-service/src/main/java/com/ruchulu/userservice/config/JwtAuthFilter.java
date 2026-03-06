package com.ruchulu.userservice.config;

import com.ruchulu.userservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request.
 * Extracts and validates the Bearer JWT from the Authorization header.
 * If valid, sets the authentication in the SecurityContext.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService     jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtService.isTokenValid(token)) {
                String userId = jwtService.extractUserId(token);
                String role   = jwtService.extractRole(token);

                // Verify user still exists and is active
                userRepository.findByIdAndDeletedFalse(userId).ifPresent(user -> {
                    if (user.isActive()) {
                        var auth = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("JWT filter error: {}", e.getMessage());
            // Don't throw — let Spring Security return 401
        }

        filterChain.doFilter(request, response);
    }
}
