package com.example.trails.security;

import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory brute-force guard for the login form.
 * Locks out a username for a cooldown period after too many failed
 * attempts in a row. This is intentionally lightweight (no extra
 * infrastructure like Redis) - state resets on app restart, which is
 * an acceptable trade-off for a small single-instance deployment.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MILLIS = 15 * 60 * 1000L; // 15 minutes

    private static class Attempts {
        AtomicInteger count = new AtomicInteger(0);
        volatile Instant lockedUntil = null;
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        if (username == null) return false;
        Attempts attempts = attemptsByUsername.get(normalize(username));
        if (attempts == null || attempts.lockedUntil == null) return false;
        if (Instant.now().isAfter(attempts.lockedUntil)) {
            // Lockout expired - reset
            attemptsByUsername.remove(normalize(username));
            return false;
        }
        return true;
    }

    public void loginFailed(String username) {
        if (username == null) return;
        Attempts attempts = attemptsByUsername.computeIfAbsent(normalize(username), k -> new Attempts());
        int failures = attempts.count.incrementAndGet();
        if (failures >= MAX_ATTEMPTS) {
            attempts.lockedUntil = Instant.now().plusMillis(LOCKOUT_DURATION_MILLIS);
        }
    }

    public void loginSucceeded(String username) {
        if (username == null) return;
        attemptsByUsername.remove(normalize(username));
    }

    private String normalize(String username) {
        return username.trim().toLowerCase();
    }
}
