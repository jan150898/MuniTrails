package com.example.trails.service;

import com.example.trails.dto.RegistrationRequest;
import com.example.trails.model.User;
import com.example.trails.model.VerificationToken;
import com.example.trails.repo.UserRepository;
import com.example.trails.repo.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegistrationService
 */
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registrationService = new RegistrationService();
        ReflectionTestUtils.setField(registrationService, "userRepository", userRepository);
        ReflectionTestUtils.setField(registrationService, "verificationTokenRepository", verificationTokenRepository);
        ReflectionTestUtils.setField(registrationService, "emailService", emailService);
        ReflectionTestUtils.setField(registrationService, "passwordEncoder", passwordEncoder);
    }

    @Test
    void testRegisterUserWithValidDataCreatesToken() {
        RegistrationRequest request = new RegistrationRequest(
                "testuser",
                "test@example.com",
                "securepass123",
                "securepass123"
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(verificationTokenRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        registrationService.registerUser(request);

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());

        VerificationToken token = captor.getValue();
        assertEquals("test@example.com", token.getEmail());
        assertNotNull(token.getToken());
        assertFalse(token.isVerified());
        assertFalse(token.isExpired());

        verify(emailService).sendVerificationEmail("test@example.com", token.getToken());
    }

    @Test
    void testRegisterUserWithPasswordMismatchThrowsError() {
        RegistrationRequest request = new RegistrationRequest(
                "testuser",
                "test@example.com",
                "securepass123",
                "differentpass123"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            registrationService.registerUser(request);
        });

        verify(verificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void testRegisterUserWithDuplicateUsernameThrowsError() {
        RegistrationRequest request = new RegistrationRequest(
                "existing",
                "test@example.com",
                "securepass123",
                "securepass123"
        );

        User existingUser = new User();
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalArgumentException.class, () -> {
            registrationService.registerUser(request);
        });

        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void testRegisterUserWithDuplicateEmailThrowsError() {
        RegistrationRequest request = new RegistrationRequest(
                "testuser",
                "existing@example.com",
                "securepass123",
                "securepass123"
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        User existingUser = new User();
        when(userRepository.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalArgumentException.class, () -> {
            registrationService.registerUser(request);
        });

        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void testVerifyEmailAndCreateUserWithValidCodeCreatesUser() {
        VerificationToken token = new VerificationToken(
                "123456",
                "test@example.com",
                Instant.now().plusSeconds(15 * 60) // 15 minutes from now
        );

        when(verificationTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("securepass123")).thenReturn("$2b$10$encoded");

        RegistrationRequest originalRequest = new RegistrationRequest(
                "testuser",
                "test@example.com",
                "securepass123",
                "securepass123"
        );

        registrationService.verifyEmailAndCreateUser("test@example.com", "123456", originalRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("USER", savedUser.getRole());

        verify(emailService).sendWelcomeEmail("test@example.com", "testuser");
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    void testVerifyEmailWithExpiredCodeThrowsError() {
        VerificationToken expiredToken = new VerificationToken(
                "123456",
                "test@example.com",
                Instant.now().minusSeconds(60) // Already expired
        );

        when(verificationTokenRepository.findByToken("123456")).thenReturn(Optional.of(expiredToken));

        RegistrationRequest originalRequest = new RegistrationRequest(
                "testuser",
                "test@example.com",
                "securepass123",
                "securepass123"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            registrationService.verifyEmailAndCreateUser("test@example.com", "123456", originalRequest);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void testVerifyEmailWithInvalidCodeThrowsError() {
        when(verificationTokenRepository.findByToken("wrongcode")).thenReturn(Optional.empty());

        RegistrationRequest originalRequest = new RegistrationRequest(
                "testuser",
                "test@example.com",
                "securepass123",
                "securepass123"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            registrationService.verifyEmailAndCreateUser("test@example.com", "wrongcode", originalRequest);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void testResendVerificationCodeSendsEmail() {
        VerificationToken token = new VerificationToken(
                "123456",
                "test@example.com",
                Instant.now().plusSeconds(15 * 60)
        );

        when(verificationTokenRepository.findByEmail("test@example.com")).thenReturn(Optional.of(token));

        registrationService.resendVerificationCode("test@example.com");

        verify(emailService).sendVerificationEmail("test@example.com", "123456");
    }

    @Test
    void testResendVerificationCodeWithNoPendingThrowsError() {
        when(verificationTokenRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            registrationService.resendVerificationCode("test@example.com");
        });

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }
}
