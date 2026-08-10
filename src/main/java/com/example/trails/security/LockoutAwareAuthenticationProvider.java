package com.example.trails.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Wraps the DB-backed authentication provider to enforce a brute-force
 * lockout before delegating to the real password check.
 */
public class LockoutAwareAuthenticationProvider implements AuthenticationProvider {

    private final DaoAuthenticationProvider delegate;
    private final LoginAttemptService loginAttemptService;

    public LockoutAwareAuthenticationProvider(DaoAuthenticationProvider delegate,
                                               LoginAttemptService loginAttemptService) {
        this.delegate = delegate;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        if (loginAttemptService.isLocked(username)) {
            throw new LockedException("Too many failed login attempts. Please try again in 15 minutes.");
        }
        return delegate.authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}
