package com.hirehub.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a rate limit is exceeded.
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}
