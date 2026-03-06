package com.ruchulu.catererservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@Slf4j
public class CatererServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatererServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  🍽️  Ruchulu Caterer Service — STARTED");
        log.info("  Base URL   : http://localhost:8082/api/v1");
        log.info("  Search     : http://localhost:8082/api/v1/caterers/search");
        log.info("  H2 Console : http://localhost:8082/api/v1/h2-console");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
