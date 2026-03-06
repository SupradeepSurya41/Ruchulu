package com.ruchulu.catererservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success;
    private String  errorCode;
    private String  message;
    private Map<String, String> fieldErrors;
    private String  path;
    private LocalDateTime timestamp;
    private String  traceId;
}
