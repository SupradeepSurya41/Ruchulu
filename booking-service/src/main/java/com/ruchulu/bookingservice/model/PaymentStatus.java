package com.ruchulu.bookingservice.model;

public enum PaymentStatus {
    UNPAID,
    ADVANCE_PAID,    // 20% advance paid
    FULLY_PAID,      // 100% paid
    REFUNDED,        // refunded after cancellation
    PARTIALLY_REFUNDED
}
