package com.example.trails.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // Demo in-memory users.
    // Admin: admin / admin123
    // User: user / user123
    public static final Map<String, DemoAccount> ACCOUNTS = Map.of(
            "admin", new DemoAccount("admin", "admin123", "ROLE_ADMIN"),
            "user", new DemoAccount("user", "user123", "ROLE_USER")
    );

    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        DemoAccount acc = ACCOUNTS.get(username);
        if (acc == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return User.withUsername(acc.username())
                .password(passwordEncoder.encode(acc.rawPassword()))
                .roles(acc.role().replace("ROLE_", ""))
                .build();
    }

    public static class DemoAccount {
        private final String username;
        private final String rawPassword;
        private final String role;

        public DemoAccount(String username, String rawPassword, String role) {
            this.username = username;
            this.rawPassword = rawPassword;
            this.role = role;
        }

        public String username() {
            return username;
        }

        public String rawPassword() {
            return rawPassword;
        }

        public String role() {
            return role;
        }
    }

}

