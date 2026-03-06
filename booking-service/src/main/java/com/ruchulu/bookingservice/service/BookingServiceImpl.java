package com.ruchulu.bookingservice.service;

import com.ruchulu.bookingservice.dto.*;
import com.ruchulu.bookingservice.exception.*;
import com.ruchulu.bookingservice.model.*;
import com.ruchulu.bookingservice.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * BookingServiceImpl — core booking business logic.
 *
 * STATE MACHINE TRANSITIONS:
 *   createBooking()   → PENDING
 *   confirmBooking()  → PENDING  → CONFIRMED
 *   rejectBooking()   → PENDING  → REJECTED
 *   cancelBooking()   → PENDING/CONFIRMED → CANCELLED
 *   markInProgress()  → CONFIRMED → IN_PROGRESS
 *   markCompleted()   → IN_PROGRESS → COMPLETED
 *   expirePending()   → PENDING (48h old) → EXPIRED (scheduled)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository      bookingRepo;
    private final BookingEventRepository eventRepo;

    @Value("${app.booking.auto-expire-hours:48}")
    private int autoExpireHours;

    @Value("${app.booking.cancellation-window-hours:24}")
    private int cancellationWindowHours;

    @Value("${app.booking.min-advance-days:1}")
    private int minAdvanceDays;

    // ── CREATE BOOKING ────────────────────────────────────────────────────
    @Override
    public Booking createBooking(String customerId, String customerName, String customerEmail,
                                  String customerPhone, CreateBookingRequest req) {
        log.info("Creating booking: customer={}, caterer={}, date={}",
            customerId, req.getCatererId(), req.getEventDate());

        // Guard: cannot book yourself
        if (customerId.equals(req.getCatererId())) {
            throw new SelfBookingException();
        }

        // Guard: advance notice
        long daysUntilEvent = ChronoUnit.DAYS.between(LocalDate.now(), req.getEventDate());
        if (daysUntilEvent < minAdvanceDays) {
            throw new InsufficientAdvanceNoticeException(minAdvanceDays);
        }

        // Guard: no duplicate booking
        if (bookingRepo.existsActiveBooking(customerId, req.getCatererId(), req.getEventDate())) {
            throw new DuplicateBookingException();
        }

        // Build pricing (use provided pricePerPlate, or fall back to 300 default)
        BigDecimal price = req.getPricePerPlate() != null
            ? req.getPricePerPlate()
            : BigDecimal.valueOf(300);

        Booking booking = Booking.builder()
                .customerId(customerId)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .catererId(req.getCatererId())
                .catererName("Caterer") // enriched from caterer-service in real flow
                .occasion(req.getOccasion())
                .eventDate(req.getEventDate())
                .eventCity(req.getEventCity())
                .eventAddress(req.getEventAddress())
                .guestCount(req.getGuestCount())
                .specialRequests(req.getSpecialRequests())
                .selectedMenuItemIds(req.getSelectedMenuItemIds() != null
                    ? req.getSelectedMenuItemIds() : List.of())
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .expiresAt(LocalDateTime.now().plusHours(autoExpireHours))
                .build();

        booking.calculateAmounts(price, req.getGuestCount());

        Booking saved = bookingRepo.save(booking);
        addEvent(saved, BookingEventType.CREATED,
            "Booking created for " + req.getOccasion() + " on " + req.getEventDate(), customerId);

        log.info("Booking created: id={}, total=₹{}", saved.getId(), saved.getTotalAmount());
        return saved;
    }

    // ── GET BOOKING ───────────────────────────────────────────────────────
    @Override
    public Booking getBookingById(String bookingId, String requestingUserId) {
        Booking b = findOrThrow(bookingId);
        if (!b.belongsToCustomer(requestingUserId) && !b.belongsToCaterer(requestingUserId)) {
            throw new UnauthorizedBookingAccessException();
        }
        return b;
    }

    // ── LIST CUSTOMER BOOKINGS ────────────────────────────────────────────
    @Override
    public Page<Booking> getMyBookings(String customerId, BookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null) {
            return bookingRepo.findByCustomerIdAndStatusAndDeletedFalse(customerId, status, pageable);
        }
        return bookingRepo.findByCustomerIdAndDeletedFalse(customerId, pageable);
    }

    // ── CANCEL BOOKING ────────────────────────────────────────────────────
    @Override
    public void cancelBooking(String bookingId, String customerId, String reason) {
        Booking b = findOrThrow(bookingId);

        if (!b.belongsToCustomer(customerId)) throw new UnauthorizedBookingAccessException();
        if (b.isTerminal())                   throw new BookingAlreadyTerminalException(b.getStatus().name());
        if (!b.isCancellable())               throw new InvalidBookingStateException(b.getStatus().name(), "CANCELLED");

        // Check 24h cancellation window for confirmed bookings
        if (b.getStatus() == BookingStatus.CONFIRMED) {
            long hoursUntilEvent = ChronoUnit.HOURS.between(LocalDateTime.now(),
                b.getEventDate().atStartOfDay());
            if (hoursUntilEvent < cancellationWindowHours) {
                throw new CancellationWindowExpiredException();
            }
        }

        b.setStatus(BookingStatus.CANCELLED);
        b.setCancellationReason(reason);
        b.setCancelledAt(LocalDateTime.now());
        bookingRepo.save(b);

        addEvent(b, BookingEventType.CANCELLED, "Cancelled by customer. Reason: " + reason, customerId);
        log.info("Booking cancelled: id={}, by={}", bookingId, customerId);
    }

    // ── RECORD PAYMENT ────────────────────────────────────────────────────
    @Override
    public void recordPayment(String bookingId, String userId, PaymentRequest req) {
        Booking b = findOrThrow(bookingId);
        if (!b.belongsToCustomer(userId)) throw new UnauthorizedBookingAccessException();

        boolean isAdvance = req.getAmount().compareTo(b.getAdvanceAmount()) == 0;
        boolean isFull    = req.getAmount().compareTo(b.getTotalAmount()) == 0;

        if (isAdvance || req.getAmount().compareTo(b.getAdvanceAmount()) >= 0) {
            b.setPaymentStatus(isFull ? PaymentStatus.FULLY_PAID : PaymentStatus.ADVANCE_PAID);
        }
        bookingRepo.save(b);

        addEvent(b, isAdvance ? BookingEventType.ADVANCE_PAID : BookingEventType.FULLY_PAID,
            "Payment of ₹" + req.getAmount() + " via " + req.getPaymentMode(), userId);
        log.info("Payment recorded for booking {}: ₹{}", bookingId, req.getAmount());
    }

    // ── CATERER: CONFIRM ──────────────────────────────────────────────────
    @Override
    public void confirmBooking(String bookingId, String catererId, String notes) {
        Booking b = findOrThrow(bookingId);
        if (!b.belongsToCaterer(catererId))      throw new UnauthorizedBookingAccessException();
        if (b.getStatus() != BookingStatus.PENDING)
            throw new InvalidBookingStateException(b.getStatus().name(), "CONFIRMED");

        b.setStatus(BookingStatus.CONFIRMED);
        b.setCatererNotes(notes);
        b.setConfirmedAt(LocalDateTime.now());
        bookingRepo.save(b);

        addEvent(b, BookingEventType.CONFIRMED, "Booking confirmed by caterer."
            + (notes != null ? " Note: " + notes : ""), catererId);
        log.info("Booking confirmed: id={}, caterer={}", bookingId, catererId);
    }

    // ── CATERER: REJECT ───────────────────────────────────────────────────
    @Override
    public void rejectBooking(String bookingId, String catererId, String reason) {
        Booking b = findOrThrow(bookingId);
        if (!b.belongsToCaterer(catererId))         throw new UnauthorizedBookingAccessException();
        if (b.getStatus() != BookingStatus.PENDING)
            throw new InvalidBookingStateException(b.getStatus().name(), "REJECTED");

        b.setStatus(BookingStatus.REJECTED);
        b.setRejectionReason(reason);
        bookingRepo.save(b);

        addEvent(b, BookingEventType.REJECTED, "Booking rejected by caterer. Reason: " + reason, catererId);
        log.info("Booking rejected: id={}, caterer={}", bookingId, catererId);
    }

    // ── CATERER: IN_PROGRESS ──────────────────────────────────────────────
    @Override
    public void markInProgress(String bookingId, String catererId) {
        Booking b = findOrThrow(bookingId);
        if (!b.belongsToCaterer(catererId))           throw new UnauthorizedBookingAccessException();
        if (b.getStatus() != BookingStatus.CONFIRMED)
            throw new InvalidBookingStateException(b.getStatus().name(), "IN_PROGRESS");

        b.setStatus(BookingStatus.IN_PROGRESS);
        bookingRepo.save(b);
        addEvent(b, BookingEventType.IN_PROGRESS, "Event is now in progress.", catererId);
    }

    // ── CATERER: COMPLETED ────────────────────────────────────────────────
    @Override
    public void markCompleted(String bookingId, String catererId) {
        Booking b = findOrThrow(bookingId);
        if (!b.belongsToCaterer(catererId))             throw new UnauthorizedBookingAccessException();
        if (b.getStatus() != BookingStatus.IN_PROGRESS)
            throw new InvalidBookingStateException(b.getStatus().name(), "COMPLETED");

        b.setStatus(BookingStatus.COMPLETED);
        b.setCompletedAt(LocalDateTime.now());
        bookingRepo.save(b);
        addEvent(b, BookingEventType.COMPLETED,
            "Event completed successfully. Review is now unlocked for the customer.", catererId);
        log.info("Booking completed: id={}", bookingId);
    }

    // ── CATERER: LIST BOOKINGS ────────────────────────────────────────────
    @Override
    public Page<Booking> getCatererBookings(String catererId, BookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").ascending());
        if (status != null) {
            return bookingRepo.findByCatererIdAndStatusAndDeletedFalse(catererId, status, pageable);
        }
        return bookingRepo.findByCatererIdAndDeletedFalse(catererId, pageable);
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────
    @Override
    public Page<Booking> getAllBookings(BookingFilterRequest filter) {
        int page = filter.getPage() != null ? filter.getPage() : 0;
        int size = filter.getSize() != null ? filter.getSize() : 20;
        return bookingRepo.findFiltered(
            filter.getStatus(), filter.getOccasion(), filter.getCity(),
            filter.getFromDate(), filter.getToDate(),
            PageRequest.of(page, size)
        );
    }

    @Override
    public void forceCancel(String bookingId, String adminId, String reason) {
        Booking b = findOrThrow(bookingId);
        if (b.isTerminal()) throw new BookingAlreadyTerminalException(b.getStatus().name());
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancellationReason("ADMIN: " + reason);
        b.setCancelledAt(LocalDateTime.now());
        bookingRepo.save(b);
        addEvent(b, BookingEventType.CANCELLED, "Force cancelled by admin. Reason: " + reason, adminId);
    }

    // ── TIMELINE ──────────────────────────────────────────────────────────
    @Override
    public List<BookingEvent> getTimeline(String bookingId, String requestingUserId) {
        Booking b = findOrThrow(bookingId);
        if (!b.belongsToCustomer(requestingUserId) && !b.belongsToCaterer(requestingUserId)) {
            throw new UnauthorizedBookingAccessException();
        }
        return eventRepo.findByBooking_IdOrderByCreatedAtAsc(bookingId);
    }

    // ── SCHEDULED: AUTO-EXPIRE ─────────────────────────────────────────────
    @Override
    @Scheduled(fixedDelay = 3600000) // every hour
    public void expirePendingBookings() {
        List<Booking> expired = bookingRepo.findExpiredPendingBookings(LocalDateTime.now());
        expired.forEach(b -> {
            b.setStatus(BookingStatus.EXPIRED);
            bookingRepo.save(b);
            addEvent(b, BookingEventType.EXPIRED,
                "Booking auto-expired after " + autoExpireHours + "h without caterer response.", "SYSTEM");
            log.info("Booking auto-expired: id={}", b.getId());
        });
        if (!expired.isEmpty()) {
            log.info("Auto-expired {} pending bookings.", expired.size());
        }
    }

    // ── SCHEDULED: AUTO IN_PROGRESS ───────────────────────────────────────
    @Override
    @Scheduled(cron = "0 0 8 * * *") // 8 AM daily
    public void autoMarkInProgress() {
        List<Booking> todayBookings = bookingRepo.findConfirmedForToday(LocalDate.now());
        todayBookings.forEach(b -> {
            b.setStatus(BookingStatus.IN_PROGRESS);
            bookingRepo.save(b);
            addEvent(b, BookingEventType.IN_PROGRESS, "Event started — marked IN_PROGRESS by system.", "SYSTEM");
        });
        if (!todayBookings.isEmpty()) {
            log.info("Auto-marked {} bookings as IN_PROGRESS for today.", todayBookings.size());
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private Booking findOrThrow(String bookingId) {
        return bookingRepo.findByIdAndDeletedFalse(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    private void addEvent(Booking booking, BookingEventType type, String description, String performedBy) {
        BookingEvent event = BookingEvent.builder()
                .booking(booking)
                .eventType(type)
                .description(description)
                .performedBy(performedBy)
                .build();
        eventRepo.save(event);
    }
}
