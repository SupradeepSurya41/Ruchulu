package com.ruchulu.bookingservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  📅  Ruchulu Booking Service — STARTED");
        log.info("  Base URL   : http://localhost:8083/api/v1");
        log.info("  Bookings   : http://localhost:8083/api/v1/bookings");
        log.info("  H2 Console : http://localhost:8083/api/v1/h2-console");
        log.info("  Scheduler  : Auto-expire every 1h | Auto-progress at 8AM daily");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
