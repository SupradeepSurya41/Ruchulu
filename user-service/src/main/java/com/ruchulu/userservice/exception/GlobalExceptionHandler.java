package com.ruchulu.userservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central exception handler — translates every exception
 * into a clean, consistent JSON error response.
 *
 * Frontend always receives an ErrorResponse body on non-2xx status.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 1. Bean validation errors (@Valid / @Validated) ─────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        ErrorResponse body = ErrorResponse.builder()
                .success(false)
                .errorCode("VALIDATION_FAILED")
                .message("One or more fields are invalid. Please correct them and try again.")
                .fieldErrors(fieldErrors)
                .path(extractPath(request))
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Validation failed at {}: {}", extractPath(request), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── 2. All custom Ruchulu domain exceptions ──────────────────────────
    @ExceptionHandler(RuchuluException.class)
    public ResponseEntity<ErrorResponse> handleRuchuluException(
            RuchuluException ex, WebRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .success(false)
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(extractPath(request))
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Domain exception [{}] at {}: {}", ex.getErrorCode(), extractPath(request), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    // ── 3. Spring Security: bad credentials ─────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            buildError("AUTH_INVALID_CREDENTIALS",
                "Invalid credentials. Please check your email/phone and password.", request));
    }

    // ── 4. Spring Security: access denied ───────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            buildError("ACCESS_DENIED",
                "You do not have permission to perform this action.", request));
    }

    // ── 5. Missing @RequestParam ─────────────────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, WebRequest request) {

        return ResponseEntity.badRequest().body(
            buildError("PARAM_MISSING",
                "Required parameter '" + ex.getParameterName() + "' is missing.", request));
    }

    // ── 6. Wrong HTTP method ─────────────────────────────────────────────
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleWrongMethod(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            buildError("METHOD_NOT_ALLOWED",
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.", request));
    }

    // ── 7. Malformed JSON body ────────────────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, WebRequest request) {

        return ResponseEntity.badRequest().body(
            buildError("MALFORMED_JSON",
                "Request body is malformed or missing. Please send valid JSON.", request));
    }

    // ── 8. Type mismatch in path/query variables ─────────────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        return ResponseEntity.badRequest().body(
            buildError("TYPE_MISMATCH",
                "Parameter '" + ex.getName() + "' has an invalid value: '" + ex.getValue() + "'.", request));
    }

    // ── 9. IllegalArgumentException ──────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument at {}: {}", extractPath(request), ex.getMessage());
        return ResponseEntity.badRequest().body(
            buildError("INVALID_ARGUMENT", ex.getMessage(), request));
    }

    // ── 10. Catch-all fallback ────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Unhandled exception [traceId={}] at {}: {}", traceId, extractPath(request), ex.getMessage(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .success(false)
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("Something went wrong on our end. Our team has been notified. " +
                         "Please try again shortly. (Reference: " + traceId + ")")
                .path(extractPath(request))
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private ErrorResponse buildError(String code, String message, WebRequest request) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(code)
                .message(message)
                .path(extractPath(request))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
