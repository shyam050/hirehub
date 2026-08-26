package com.hirehub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation ID to every request and logs method, URI, status, and duration.
 * The correlation ID is returned in the X-Request-ID response header.
 */
@Slf4j
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // Only log non-health/metrics endpoints to reduce noise
            if (!uri.startsWith("/actuator")) {
                String userId = getUserFromRequest(request);
                if (userId != null) {
                    log.info("[{}] {} {} {} {}ms user={}", requestId, method, uri, status, duration, userId);
                } else {
                    log.info("[{}] {} {} {} {}ms", requestId, method, uri, status, duration);
                }
            }
        }
    }

    private String getUserFromRequest(HttpServletRequest request) {
        Object principal = request.getAttribute("userEmail");
        if (principal instanceof String s) return s;
        return null;
    }

    public static String getRequestId(HttpServletRequest request) {
        Object id = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        return id instanceof String s ? s : "unknown";
    }
}
