package com.ruchulu.notificationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  📧  Ruchulu Notification Service — STARTED");
        log.info("  Base URL   : http://localhost:8084/api/v1");
        log.info("  Send API   : http://localhost:8084/api/v1/notifications/send");
        log.info("  H2 Console : http://localhost:8084/api/v1/h2-console");
        log.info("  Channels   : EMAIL | SMS (stub) | PUSH (stub)");
        log.info("  Retry      : every 5 min for PENDING/RETRYING");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
