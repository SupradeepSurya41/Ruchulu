package com.ruchulu.apigateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@Slf4j
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  🌐  Ruchulu API Gateway — STARTED on port 8080");
        log.info("");
        log.info("  Route Map:");
        log.info("  /api/v1/auth/**          → user-service    (8081) [PUBLIC]");
        log.info("  /api/v1/otp/**           → user-service    (8081) [PUBLIC]");
        log.info("  /api/v1/users/**         → user-service    (8081) [JWT]");
        log.info("  /api/v1/caterers/search  → caterer-service (8082) [PUBLIC]");
        log.info("  /api/v1/caterers/**      → caterer-service (8082) [JWT]");
        log.info("  /api/v1/bookings/**      → booking-service (8083) [JWT]");
        log.info("  /api/v1/notifications/** → notification-service (8084) [JWT/INTERNAL]");
        log.info("");
        log.info("  Features: JWT validation | Rate limiting | Circuit breaker | Logging");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
