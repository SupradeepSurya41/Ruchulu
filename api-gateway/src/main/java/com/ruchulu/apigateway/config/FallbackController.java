package com.ruchulu.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * FallbackController — circuit breaker fallback responses.
 * Called when a downstream service is unreachable.
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @RequestMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("Circuit breaker triggered for user-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "success", false,
            "errorCode", "SERVICE_UNAVAILABLE",
            "message", "User Service is currently unavailable. Please try again shortly.",
            "service", "user-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @RequestMapping("/caterer-service")
    public ResponseEntity<Map<String, Object>> catererServiceFallback() {
        log.warn("Circuit breaker triggered for caterer-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "success", false,
            "errorCode", "SERVICE_UNAVAILABLE",
            "message", "Caterer Service is currently unavailable. Please try again shortly.",
            "service", "caterer-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @RequestMapping("/booking-service")
    public ResponseEntity<Map<String, Object>> bookingServiceFallback() {
        log.warn("Circuit breaker triggered for booking-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "success", false,
            "errorCode", "SERVICE_UNAVAILABLE",
            "message", "Booking Service is currently unavailable. Please try again shortly.",
            "service", "booking-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @RequestMapping("/notification-service")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        log.warn("Circuit breaker triggered for notification-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "success", false,
            "errorCode", "SERVICE_UNAVAILABLE",
            "message", "Notification Service is temporarily unavailable.",
            "service", "notification-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
