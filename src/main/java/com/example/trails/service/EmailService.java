package com.example.trails.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.email.from:noreply@munitrails.local}")
    private String fromEmail;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    public void sendVerificationEmail(String toEmail, String code) {
        if (!emailEnabled) {
            logger.warn("Email sending is disabled. Verification code for {}: {}", toEmail, code);
            return;
        }

        if (mailSender == null) {
            logger.warn("JavaMailSender not configured. Skipping email to {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("MuniTrails - Email Verification");
            message.setText(buildVerificationEmailBody(code));

            mailSender.send(message);
            logger.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String username) {
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email disabled or not configured. Skipping welcome email to {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to MuniTrails!");
            message.setText(buildWelcomeEmailBody(username));

            mailSender.send(message);
            logger.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}", toEmail, e);
            // Don't throw - this is non-critical
        }
    }

    private String buildVerificationEmailBody(String code) {
        return "Your MuniTrails email verification code is:\n\n" +
               code + "\n\n" +
               "This code will expire in 15 minutes.\n\n" +
               "If you did not request this, please ignore this email.\n\n" +
               "Best regards,\nMuniTrails Team";
    }

    private String buildWelcomeEmailBody(String username) {
        return "Welcome to MuniTrails, " + username + "!\n\n" +
               "Your account has been created successfully.\n" +
               "You can now log in and start tracking your mountain bike trails.\n\n" +
               "Happy riding!\n\n" +
               "Best regards,\nMuniTrails Team";
    }
}
