package com.gametime.backend.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class LoginAttemptTracker {

    private static final long MAX_DELAY_MILLIS = Duration.ofMinutes(5).toMillis();

    private int failures;
    private Instant allowedAgainAt = Instant.EPOCH;

    public synchronized long remainingDelayMillis() {
        return Math.max(0, Duration.between(Instant.now(), allowedAgainAt).toMillis());
    }

    public synchronized void recordFailure() {
        failures = Math.min(failures + 1, 10);
        long delayMillis = Math.min(MAX_DELAY_MILLIS, 1_000L << Math.min(failures - 1, 8));
        allowedAgainAt = Instant.now().plusMillis(delayMillis);
    }

    public synchronized void recordSuccess() {
        failures = 0;
        allowedAgainAt = Instant.EPOCH;
    }
}
