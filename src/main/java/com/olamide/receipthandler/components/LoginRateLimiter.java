package com.olamide.receipthandler.components;

import com.olamide.receipthandler.exceptions.TooManyLoginAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Window(int count, Instant windowStart) {
        boolean isExpired() {
            return Instant.now().isAfter(windowStart.plus(WINDOW));
        }
    }

    private final ConcurrentHashMap<String, Window> attemptsByKey = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        Window window = attemptsByKey.get(key);
        if (window != null && !window.isExpired() && window.count() >= MAX_ATTEMPTS) {
            throw new TooManyLoginAttemptsException(
                    "Too many failed login attempts. Please try again in a few minutes.");
        }
    }

    public void recordFailure(String key) {
        attemptsByKey.compute(key, (k, existing) ->
                (existing == null || existing.isExpired())
                        ? new Window(1, Instant.now())
                        : new Window(existing.count() + 1, existing.windowStart()));
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(key);
    }
}
