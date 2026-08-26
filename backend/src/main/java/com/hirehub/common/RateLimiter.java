package com.hirehub.common;

import com.hirehub.common.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for single-instance deployments.
 * Uses a sliding window counter approach per key (typically IP + endpoint).
 */
@Slf4j
@Component
public class RateLimiter {

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.auth-max:10}")
    private int authMax;

    @Value("${rate-limit.ai-max:5}")
    private int aiMax;

    @Value("${rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public void checkAuthRateLimit(String key) {
        if (!enabled) return;
        check(key, authMax);
    }

    public void checkAiRateLimit(String key) {
        if (!enabled) return;
        check(key, aiMax);
    }

    public void check(String key, int maxRequests) {
        if (!enabled) return;

        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart < windowStart) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > maxRequests) {
            log.warn("Rate limit exceeded for key: {}, count: {}, max: {}", key, counter.count.get(), maxRequests);
            throw new RateLimitException("Too many requests. Please try again later.");
        }
    }

    /**
     * Evict stale entries periodically (called from scheduled task or on access).
     */
    public void evictStale() {
        long cutoff = System.currentTimeMillis() - (windowSeconds * 2 * 1000L);
        counters.entrySet().removeIf(entry -> entry.getValue().windowStart < cutoff);
    }

    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger count;

        WindowCounter(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
