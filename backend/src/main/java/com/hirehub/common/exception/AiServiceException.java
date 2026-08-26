package com.hirehub.common.exception;

/**
 * Thrown when the AI service (OpenAI) is unavailable or fails after retries.
 * Maps to HTTP 503 Service Unavailable.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
