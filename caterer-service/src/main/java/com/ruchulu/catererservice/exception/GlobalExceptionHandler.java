package com.ruchulu.catererservice.exception;

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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
            fieldErrors.put(((FieldError) e).getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .success(false).errorCode("VALIDATION_FAILED")
                .message("One or more fields are invalid.")
                .fieldErrors(fieldErrors)
                .path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    @ExceptionHandler(CatererException.class)
    public ResponseEntity<ErrorResponse> handleCatererException(CatererException ex, WebRequest req) {
        log.warn("Domain exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorResponse.builder()
                .success(false).errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest req) {
        String traceId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Unhandled [traceId={}]: {}", traceId, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .success(false).errorCode("INTERNAL_SERVER_ERROR")
                .message("Something went wrong. Reference: " + traceId)
                .traceId(traceId)
                .path(path(req)).timestamp(LocalDateTime.now()).build());
    }

    private String path(WebRequest req) {
        return req.getDescription(false).replace("uri=", "");
    }
}
