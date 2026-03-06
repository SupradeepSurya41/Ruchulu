package com.ruchulu.bookingservice.exception;

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

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ErrorResponse {
    private boolean success;
    private String  errorCode;
    private String  message;
    private Map<String, String> fieldErrors;
    private String  path;
    private LocalDateTime timestamp;
    private String  traceId;
}

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> fe = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
            fe.put(((FieldError) e).getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .success(false).errorCode("VALIDATION_FAILED")
                .message("One or more fields are invalid.")
                .fieldErrors(fe).path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ErrorResponse> handleBookingException(BookingException ex, WebRequest req) {
        log.warn("Booking exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorResponse.builder()
                .success(false).errorCode(ex.getErrorCode())
                .message(ex.getMessage()).path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest req) {
        String tid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Unhandled [traceId={}]: {}", tid, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .success(false).errorCode("INTERNAL_SERVER_ERROR")
                .message("Something went wrong. Ref: " + tid)
                .traceId(tid).path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    private String path(WebRequest r) { return r.getDescription(false).replace("uri=", ""); }
}
