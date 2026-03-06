package com.ruchulu.bookingservice.repository;

import com.ruchulu.bookingservice.model.BookingEvent;
import com.ruchulu.bookingservice.model.BookingEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingEventRepository extends JpaRepository<BookingEvent, String> {
    List<BookingEvent> findByBooking_IdOrderByCreatedAtAsc(String bookingId);
    List<BookingEvent> findByBooking_IdAndEventType(String bookingId, BookingEventType type);
    long countByBooking_Id(String bookingId);
}
