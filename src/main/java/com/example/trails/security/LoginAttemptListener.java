package com.example.trails.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptListener {

    private final LoginAttemptService loginAttemptService;

    public LoginAttemptListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = String.valueOf(event.getAuthentication().getName());
        loginAttemptService.loginFailed(username);
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = String.valueOf(event.getAuthentication().getName());
        loginAttemptService.loginSucceeded(username);
    }
}
