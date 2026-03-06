package com.ruchulu.notificationservice.model;

public enum NotificationStatus {
    PENDING,    // queued, not yet sent
    SENT,       // successfully delivered
    FAILED,     // delivery failed after all retries
    RETRYING,   // currently being retried
    SKIPPED     // skipped due to user preference
}
