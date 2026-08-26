package com.hirehub.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
    private final String requestId;

    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, null, message, path, null);
    }

    public ErrorResponse(int status, String error, String code, String message, String path, String requestId) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
    }
}
