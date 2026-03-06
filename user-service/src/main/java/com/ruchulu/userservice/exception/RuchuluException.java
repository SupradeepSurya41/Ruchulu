package com.ruchulu.userservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all Ruchulu custom exceptions.
 * Every domain exception extends this class.
 */
public class RuchuluException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String     errorCode;

    public RuchuluException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode  = errorCode;
    }

    public RuchuluException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode  = errorCode;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getErrorCode()      { return errorCode; }
}
