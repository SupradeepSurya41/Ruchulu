package com.ruchulu.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * BookingEvent — immutable audit record for every status change or action on a booking.
 * Provides a full history timeline for customers, caterers and admins.
 */
@Entity
@Table(
    name = "booking_events",
    indexes = {
        @Index(name = "idx_event_booking", columnList = "booking_id"),
        @Index(name = "idx_event_type",    columnList = "event_type"),
        @Index(name = "idx_event_created", columnList = "created_at")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class BookingEvent {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private BookingEventType eventType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;   // userId or "SYSTEM"

    @Column(name = "metadata", length = 1000)
    private String metadata;      // JSON string for extra info (e.g. payment details)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
