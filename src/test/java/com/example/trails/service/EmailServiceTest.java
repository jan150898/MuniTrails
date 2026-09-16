package com.example.trails.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService
 */
@SpringBootTest
@ActiveProfiles("test")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@munitrails.local");
    }

    @Test
    void testSendVerificationEmailWhenEnabledAndConfigured() {
        emailService.sendVerificationEmail("test@example.com", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendVerificationEmailContainsCode() {
        emailService.sendVerificationEmail("test@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        
        SimpleMailMessage message = captor.getValue();
        assert message.getText().contains("123456");
    }

    @Test
    void testSendVerificationEmailDoesNothingWhenDisabled() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        emailService.sendVerificationEmail("test@example.com", "123456");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendVerificationEmailDoesNothingWhenMailSenderNull() {
        ReflectionTestUtils.setField(emailService, "mailSender", null);
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);

        emailService.sendVerificationEmail("test@example.com", "123456");

        // Should not throw exception
    }

    @Test
    void testSendWelcomeEmailWhenEnabledAndConfigured() {
        emailService.sendWelcomeEmail("test@example.com", "testuser");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendWelcomeEmailContainsUsername() {
        emailService.sendWelcomeEmail("test@example.com", "testuser");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        
        SimpleMailMessage message = captor.getValue();
        assert message.getText().contains("testuser");
    }

    @Test
    void testSendWelcomeEmailDoesNothingWhenDisabled() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        emailService.sendWelcomeEmail("test@example.com", "testuser");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendWelcomeEmailDoesNotThrowWhenMailSenderNull() {
        ReflectionTestUtils.setField(emailService, "mailSender", null);
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);

        // Should not throw exception
        emailService.sendWelcomeEmail("test@example.com", "testuser");
    }

    @Test
    void testVerificationEmailHasCorrectSubject() {
        emailService.sendVerificationEmail("test@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        
        SimpleMailMessage message = captor.getValue();
        assert message.getSubject().contains("MuniTrails");
        assert message.getSubject().contains("Verification");
    }

    @Test
    void testWelcomeEmailHasCorrectSubject() {
        emailService.sendWelcomeEmail("test@example.com", "testuser");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        
        SimpleMailMessage message = captor.getValue();
        assert message.getSubject().contains("MuniTrails");
        assert message.getSubject().contains("Welcome");
    }

    @Test
    void testEmailsSentFromCorrectAddress() {
        emailService.sendVerificationEmail("test@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        
        SimpleMailMessage message = captor.getValue();
        assert "noreply@munitrails.local".equals(message.getFrom());
    }

    @Test
    void testEmailsSentToCorrectAddress() {
        emailService.sendVerificationEmail("test@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        
        SimpleMailMessage message = captor.getValue();
        assert message.getTo()[0].equals("test@example.com");
    }
}
