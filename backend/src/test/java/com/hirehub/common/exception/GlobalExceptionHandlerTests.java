package com.hirehub.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void resourceNotFoundReturns404() {
        var ex = new ResourceNotFoundException("User", "id", "123");
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getCode());
        assertEquals("User not found with id: 123", response.getBody().getMessage());
    }

    @Test
    void duplicateReturns409() {
        var ex = new DuplicateResourceException("Email already registered");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("DUPLICATE_RESOURCE", response.getBody().getCode());
    }

    @Test
    void unauthorizedReturns401() {
        var ex = new UnauthorizedException("Authentication required");
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("UNAUTHORIZED", response.getBody().getCode());
    }

    @Test
    void forbiddenReturns403() {
        var ex = new ForbiddenException("Insufficient permissions");
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("FORBIDDEN", response.getBody().getCode());
    }

    @Test
    void illegalStateReturns400() {
        var ex = new com.hirehub.common.exception.IllegalStateException("Cannot apply to closed job");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("ILLEGAL_STATE", response.getBody().getCode());
    }

    @Test
    void genericExceptionReturns500() {
        var ex = new RuntimeException("Something unexpected");
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        // Must not leak internal details
        assertFalse(response.getBody().getMessage().contains("Something unexpected"));
    }

    @Test
    void validationErrorReturnsFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "email", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().get("code"));
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("fieldErrors");
        assertEquals("must not be blank", fieldErrors.get("email"));
    }

    @Test
    void aiServiceExceptionReturns503() {
        var ex = new com.hirehub.ai.AiServiceException("AI unavailable");
        ResponseEntity<ErrorResponse> response = handler.handleAiService(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, response.getBody().getStatus());
        assertEquals("AI_SERVICE_UNAVAILABLE", response.getBody().getCode());
        // Must not expose provider details
        assertFalse(response.getBody().getMessage().contains("AI unavailable"));
    }

    @Test
    void errorResponseIncludesRequestId() {
        when(request.getAttribute("requestId")).thenReturn("test-123");
        var ex = new ResourceNotFoundException("User", "id", "123");
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertEquals("test-123", response.getBody().getRequestId());
    }
}
