package com.ruchulu.bookingservice.service;

import com.ruchulu.bookingservice.dto.*;
import com.ruchulu.bookingservice.model.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BookingService {

    // Customer operations
    Booking createBooking(String customerId, String customerName, String customerEmail,
                          String customerPhone, CreateBookingRequest request);
    Booking getBookingById(String bookingId, String requestingUserId);
    Page<Booking> getMyBookings(String customerId, BookingStatus status, int page, int size);
    void cancelBooking(String bookingId, String customerId, String reason);
    void recordPayment(String bookingId, String userId, PaymentRequest request);

    // Caterer operations
    Page<Booking> getCatererBookings(String catererId, BookingStatus status, int page, int size);
    void confirmBooking(String bookingId, String catererId, String notes);
    void rejectBooking(String bookingId, String catererId, String reason);
    void markInProgress(String bookingId, String catererId);
    void markCompleted(String bookingId, String catererId);

    // Admin operations
    Page<Booking> getAllBookings(BookingFilterRequest filter);
    void forceCancel(String bookingId, String adminId, String reason);

    // System (scheduled tasks)
    void expirePendingBookings();
    void autoMarkInProgress();

    // Timeline
    List<BookingEvent> getTimeline(String bookingId, String requestingUserId);
}
