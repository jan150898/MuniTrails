package com.example.trails.service;

import com.example.trails.dto.RegistrationRequest;
import com.example.trails.model.User;
import com.example.trails.model.VerificationToken;
import com.example.trails.repo.UserRepository;
import com.example.trails.repo.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Service
@Transactional
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);
    private static final int VERIFICATION_TOKEN_VALIDITY_MINUTES = 15;
    private static final Random random = new Random();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new user and send verification email
     */
    public void registerUser(RegistrationRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if username exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email is already registered
        if (userRepository.findByUsername(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Check if verification token exists (still pending)
        verificationTokenRepository.findByEmail(request.getEmail()).ifPresent(token -> {
            if (!token.isExpired()) {
                throw new IllegalArgumentException("Verification already in progress for this email. Check your inbox or wait 15 minutes.");
            }
            // Delete expired token
            verificationTokenRepository.delete(token);
        });

        // Generate 6-digit verification code
        String verificationCode = String.format("%06d", random.nextInt(999999));

        // Create and save verification token
        Instant expiresAt = Instant.now().plusSeconds(VERIFICATION_TOKEN_VALIDITY_MINUTES * 60);
        VerificationToken token = new VerificationToken(verificationCode, request.getEmail(), expiresAt);
        verificationTokenRepository.save(token);

        // Send verification email
        emailService.sendVerificationEmail(request.getEmail(), verificationCode);

        logger.info("Registration initiated for username: {}, email: {}", request.getUsername(), request.getEmail());
    }

    /**
     * Verify email and create user account
     */
    public void verifyEmailAndCreateUser(String email, String code, RegistrationRequest originalRequest) {
        // Find verification token
        VerificationToken token = verificationTokenRepository.findByToken(code)
            .orElseThrow(() -> new IllegalArgumentException("Invalid verification code"));

        // Validate email matches
        if (!token.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Email does not match verification token");
        }

        // Check if expired
        if (token.isExpired()) {
            verificationTokenRepository.delete(token);
            throw new IllegalArgumentException("Verification code has expired. Please register again.");
        }

        // Check if already verified
        if (token.isVerified()) {
            throw new IllegalArgumentException("This email has already been verified");
        }

        // Create user account
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(originalRequest.getUsername());
        user.setPassword(passwordEncoder.encode(originalRequest.getPassword()));
        user.setRole("USER");
        userRepository.save(user);

        // Mark token as verified and delete it
        token.setVerified(true);
        verificationTokenRepository.delete(token);

        // Send welcome email
        emailService.sendWelcomeEmail(email, originalRequest.getUsername());

        logger.info("User created successfully: {} ({})", originalRequest.getUsername(), email);
    }

    /**
     * Resend verification code
     */
    public void resendVerificationCode(String email) {
        VerificationToken token = verificationTokenRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No pending verification for this email"));

        if (token.isExpired() || token.isVerified()) {
            verificationTokenRepository.delete(token);
            throw new IllegalArgumentException("Verification session has ended. Please register again.");
        }

        // Resend the code
        emailService.sendVerificationEmail(email, token.getToken());
        logger.info("Verification code resent to: {}", email);
    }
}
