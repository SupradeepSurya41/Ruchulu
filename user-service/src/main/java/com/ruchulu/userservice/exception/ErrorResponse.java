package com.ruchulu.userservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error JSON returned for ALL API errors.
 *
 * Example:
 * {
 *   "success": false,
 *   "errorCode": "USER_EMAIL_DUPLICATE",
 *   "message": "An account with email 'x@gmail.com' already exists.",
 *   "fieldErrors": { "email": "already registered" },
 *   "path": "/api/v1/auth/register",
 *   "timestamp": "2026-03-05T10:30:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private String  errorCode;
    private String  message;

    /** Populated only for @Valid bean validation failures */
    private Map<String, String> fieldErrors;

    private String        path;
    private LocalDateTime timestamp;
    private String        traceId;   // for prod log correlation
}
