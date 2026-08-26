package com.hirehub.ai;

/**
 * Exception thrown when the AI service fails.
 * Wraps provider-specific errors without exposing internals.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
