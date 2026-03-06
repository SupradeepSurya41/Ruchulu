package com.ruchulu.notificationservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// ── Error Response ────────────────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ErrorResponse {
    private boolean success;
    private String errorCode;
    private String message;
    private Map<String, String> fieldErrors;
    private String path;
    private LocalDateTime timestamp;
    private String traceId;
}

// ── Base Exception ────────────────────────────────────────────────────────
public class NotificationException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;
    public NotificationException(String msg, HttpStatus s, String c) {
        super(msg); this.httpStatus = s; this.errorCode = c;
    }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getErrorCode()      { return errorCode; }
}

// ── Domain Exceptions ─────────────────────────────────────────────────────
class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException(String id) {
        super("Notification '" + id + "' not found.", HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND");
    }
}

class NotificationSendException extends NotificationException {
    public NotificationSendException(String detail) {
        super("Failed to send notification: " + detail, HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION_SEND_FAILED");
    }
}

class InvalidTemplateException extends NotificationException {
    public InvalidTemplateException(String type) {
        super("No email template found for notification type: " + type,
              HttpStatus.BAD_REQUEST, "TEMPLATE_NOT_FOUND");
    }
}

class RecipientNotFoundException extends NotificationException {
    public RecipientNotFoundException(String recipientId) {
        super("Recipient '" + recipientId + "' not found or missing contact info.",
              HttpStatus.NOT_FOUND, "RECIPIENT_NOT_FOUND");
    }
}

// ── Global Handler ────────────────────────────────────────────────────────
@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> fe = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
            fe.put(((FieldError) e).getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .success(false).errorCode("VALIDATION_FAILED")
                .message("Validation failed").fieldErrors(fe)
                .path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotification(NotificationException ex, WebRequest req) {
        log.warn("Notification exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorResponse.builder()
                .success(false).errorCode(ex.getErrorCode()).message(ex.getMessage())
                .path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest req) {
        String tid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Unhandled [{}]: {}", tid, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .success(false).errorCode("INTERNAL_ERROR")
                .message("Unexpected error. Ref: " + tid).traceId(tid)
                .path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    private String path(WebRequest r) { return r.getDescription(false).replace("uri=", ""); }
}
