package com.ruchulu.catererservice.exception;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Caterer Custom Exceptions — Status & Message Tests")
class CatererExceptionTest {

    @Test @DisplayName("CatererNotFoundException — 404, contains id")
    void catererNotFound() {
        var ex = new CatererNotFoundException("cat-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("CATERER_NOT_FOUND");
        assertThat(ex.getMessage()).contains("cat-001");
    }

    @Test @DisplayName("CatererAlreadyExistsException — 409 CONFLICT")
    void catererAlreadyExists() {
        var ex = new CatererAlreadyExistsException("usr-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("CATERER_ALREADY_EXISTS");
    }

    @Test @DisplayName("CatererNotApprovedException — 403 FORBIDDEN")
    void catererNotApproved() {
        var ex = new CatererNotApprovedException("cat-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getErrorCode()).isEqualTo("CATERER_NOT_APPROVED");
    }

    @Test @DisplayName("GuestCountOutOfRangeException — 400, mentions numbers")
    void guestCountOutOfRange() {
        var ex = new GuestCountOutOfRangeException(800, 50, 500);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("800").contains("50").contains("500");
    }

    @Test @DisplayName("OccasionNotSupportedException — 400, mentions occasion")
    void occasionNotSupported() {
        var ex = new OccasionNotSupportedException("FUNERAL", "Good Caterers");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("FUNERAL").contains("Good Caterers");
    }

    @Test @DisplayName("MenuItemNotFoundException — 404")
    void menuItemNotFound() {
        var ex = new MenuItemNotFoundException("menu-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("MENU_ITEM_NOT_FOUND");
    }

    @Test @DisplayName("ReviewAlreadyExistsException — 409, contains bookingId")
    void reviewAlreadyExists() {
        var ex = new ReviewAlreadyExistsException("bk-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("bk-001");
    }

    @Test @DisplayName("InvalidFssaiNumberException — 400, contains number")
    void invalidFssai() {
        var ex = new InvalidFssaiNumberException("INVALID");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("INVALID");
    }

    @Test @DisplayName("CityNotSupportedException — 400, contains city name")
    void cityNotSupported() {
        var ex = new CityNotSupportedException("Mumbai");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("Mumbai");
    }

    @Test @DisplayName("UnauthorizedCatererAccessException — 403")
    void unauthorizedAccess() {
        var ex = new UnauthorizedCatererAccessException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getErrorCode()).isEqualTo("CATERER_ACCESS_DENIED");
    }

    @Test @DisplayName("InsufficientAdvanceNoticeException — 400, contains days")
    void insufficientNotice() {
        var ex = new InsufficientAdvanceNoticeException(3);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("3");
    }

    @Test @DisplayName("CatererSuspendedException — 403")
    void catererSuspended() {
        var ex = new CatererSuspendedException();
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getErrorCode()).isEqualTo("CATERER_SUSPENDED");
    }
}
