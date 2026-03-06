package com.ruchulu.bookingservice.repository;

import com.ruchulu.bookingservice.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("h2")
@DisplayName("BookingRepository — H2 Slice Tests")
class BookingRepositoryTest {

    @Autowired private BookingRepository repo;

    private Booking saved;

    @BeforeEach
    void setup() {
        saved = repo.save(Booking.builder()
                .customerId("usr-001").customerName("Ravi")
                .customerEmail("ravi@gmail.com").customerPhone("9876543210")
                .catererId("cat-001").catererName("Good Caterers")
                .occasion(OccasionType.WEDDING)
                .eventDate(LocalDate.now().plusDays(30))
                .eventCity("Hyderabad").eventAddress("Banjara Hills")
                .guestCount(200)
                .pricePerPlate(BigDecimal.valueOf(300))
                .totalAmount(BigDecimal.valueOf(60000))
                .advanceAmount(BigDecimal.valueOf(12000))
                .balanceAmount(BigDecimal.valueOf(48000))
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .deleted(false)
                .build());
    }

    @AfterEach void cleanup() { repo.deleteAll(); }

    @Test @DisplayName("findByIdAndDeletedFalse — found")
    void findById_found() {
        assertThat(repo.findByIdAndDeletedFalse(saved.getId())).isPresent();
    }

    @Test @DisplayName("findByIdAndDeletedFalse — deleted booking not found")
    void findById_deleted_notFound() {
        saved.setDeleted(true);
        repo.save(saved);
        assertThat(repo.findByIdAndDeletedFalse(saved.getId())).isEmpty();
    }

    @Test @DisplayName("findByCustomerIdAndDeletedFalse — returns customer's bookings")
    void findByCustomerId_found() {
        var page = repo.findByCustomerIdAndDeletedFalse("usr-001", PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).getCustomerId()).isEqualTo("usr-001");
    }

    @Test @DisplayName("findByCustomerIdAndStatusAndDeletedFalse — filters by status")
    void findByCustomerStatus_filters() {
        var page = repo.findByCustomerIdAndStatusAndDeletedFalse(
            "usr-001", BookingStatus.PENDING, PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(b -> b.getStatus() == BookingStatus.PENDING);
    }

    @Test @DisplayName("findByCatererIdAndDeletedFalse — returns caterer's bookings")
    void findByCatererId_found() {
        var page = repo.findByCatererIdAndDeletedFalse("cat-001", PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test @DisplayName("existsActiveBooking — true for same customer+caterer+date")
    void existsActiveBooking_true() {
        assertThat(repo.existsActiveBooking("usr-001", "cat-001",
            LocalDate.now().plusDays(30))).isTrue();
    }

    @Test @DisplayName("existsActiveBooking — false for different date")
    void existsActiveBooking_differentDate_false() {
        assertThat(repo.existsActiveBooking("usr-001", "cat-001",
            LocalDate.now().plusDays(99))).isFalse();
    }

    @Test @DisplayName("existsActiveBooking — false after cancellation")
    void existsActiveBooking_cancelled_false() {
        saved.setStatus(BookingStatus.CANCELLED);
        repo.save(saved);
        assertThat(repo.existsActiveBooking("usr-001", "cat-001",
            LocalDate.now().plusDays(30))).isFalse();
    }

    @Test @DisplayName("isCatererBooked — false for PENDING (only CONFIRMED/IN_PROGRESS count)")
    void isCatererBooked_pending_false() {
        assertThat(repo.isCatererBooked("cat-001",
            LocalDate.now().plusDays(30))).isFalse();
    }

    @Test @DisplayName("isCatererBooked — true for CONFIRMED booking on same date")
    void isCatererBooked_confirmed_true() {
        saved.setStatus(BookingStatus.CONFIRMED);
        repo.save(saved);
        assertThat(repo.isCatererBooked("cat-001",
            LocalDate.now().plusDays(30))).isTrue();
    }

    @Test @DisplayName("findExpiredPendingBookings — finds bookings past expiry")
    void findExpired_found() {
        saved.setExpiresAt(LocalDateTime.now().minusHours(1));
        repo.save(saved);
        List<Booking> expired = repo.findExpiredPendingBookings(LocalDateTime.now());
        assertThat(expired).anyMatch(b -> b.getId().equals(saved.getId()));
    }

    @Test @DisplayName("findConfirmedForToday — finds CONFIRMED bookings with today's date")
    void findConfirmedForToday_found() {
        saved.setStatus(BookingStatus.CONFIRMED);
        saved.setEventDate(LocalDate.now());
        repo.save(saved);
        List<Booking> today = repo.findConfirmedForToday(LocalDate.now());
        assertThat(today).anyMatch(b -> b.getId().equals(saved.getId()));
    }

    @Test @DisplayName("countByCustomerIdAndDeletedFalse — counts correctly")
    void countByCustomer_correct() {
        assertThat(repo.countByCustomerIdAndDeletedFalse("usr-001")).isGreaterThanOrEqualTo(1);
    }

    @Test @DisplayName("save — auto-assigns UUID id")
    void save_assignsUuid() {
        assertThat(saved.getId()).isNotNull().isNotBlank();
    }

    @Test @DisplayName("findFiltered — status filter returns matching bookings")
    void findFiltered_statusFilter() {
        var page = repo.findFiltered(BookingStatus.PENDING, null, null, null, null,
            PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(b -> b.getStatus() == BookingStatus.PENDING);
    }

    @Test @DisplayName("findFiltered — city filter is case-insensitive")
    void findFiltered_cityFilter() {
        var page = repo.findFiltered(null, null, "HYDERABAD", null, null,
            PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(b ->
            b.getEventCity().equalsIgnoreCase("Hyderabad"));
    }
}
