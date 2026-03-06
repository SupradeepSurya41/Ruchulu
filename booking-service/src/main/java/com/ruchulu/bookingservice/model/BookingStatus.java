package com.ruchulu.bookingservice.model;

/**
 * Booking State Machine:
 *
 *   PENDING ──────────────► CONFIRMED ──────► IN_PROGRESS ──────► COMPLETED
 *      │                        │                                      │
 *      │                        ▼                                      ▼
 *      └──────────────────► CANCELLED                           (review unlocked)
 *                               ▲
 *                               │
 *                           REJECTED (by caterer)
 *
 * Valid transitions:
 *   PENDING      → CONFIRMED, CANCELLED, REJECTED, EXPIRED
 *   CONFIRMED    → IN_PROGRESS, CANCELLED
 *   IN_PROGRESS  → COMPLETED
 *   COMPLETED    → (terminal — cannot change)
 *   CANCELLED    → (terminal)
 *   REJECTED     → (terminal)
 *   EXPIRED      → (terminal — auto after 48h)
 */
public enum BookingStatus {
    PENDING,        // customer submitted, awaiting caterer confirmation
    CONFIRMED,      // caterer accepted
    IN_PROGRESS,    // event day — caterer is serving
    COMPLETED,      // event done — review unlocked
    CANCELLED,      // cancelled by customer or caterer
    REJECTED,       // caterer declined
    EXPIRED         // auto-expired after 48h without caterer response
}
