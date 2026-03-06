package com.ruchulu.notificationservice.exception;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Notification Exceptions — Status & Code Tests")
class NotificationExceptionTest {

    @Test @DisplayName("NotificationNotFoundException — 404")
    void notFound() {
        var ex = new NotificationNotFoundException("n-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("NOTIFICATION_NOT_FOUND");
        assertThat(ex.getMessage()).contains("n-001");
    }

    @Test @DisplayName("NotificationSendException — 500")
    void sendFailed() {
        var ex = new NotificationSendException("SMTP timeout");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getErrorCode()).isEqualTo("NOTIFICATION_SEND_FAILED");
        assertThat(ex.getMessage()).contains("SMTP timeout");
    }

    @Test @DisplayName("InvalidTemplateException — 400, contains type")
    void invalidTemplate() {
        var ex = new InvalidTemplateException("UNKNOWN_TYPE");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("TEMPLATE_NOT_FOUND");
        assertThat(ex.getMessage()).contains("UNKNOWN_TYPE");
    }

    @Test @DisplayName("RecipientNotFoundException — 404, contains id")
    void recipientNotFound() {
        var ex = new RecipientNotFoundException("usr-001");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).contains("usr-001");
    }
}
