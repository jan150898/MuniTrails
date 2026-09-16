package com.example.trails.web;

import com.example.trails.dto.RegistrationRequest;
import com.example.trails.dto.VerificationRequest;
import com.example.trails.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private RegistrationService registrationService;

    /**
     * Show registration form
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "auth/register";
    }

    /**
     * Handle registration form submission
     */
    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute RegistrationRequest registrationRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (registrationRequest.getPassword() != null
                && !registrationRequest.getPassword().equals(registrationRequest.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "error.passwordMismatch", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            registrationService.registerUser(registrationRequest);
            redirectAttributes.addFlashAttribute("message", 
                "Registration successful! A verification code has been sent to " + registrationRequest.getEmail());
            redirectAttributes.addFlashAttribute("email", registrationRequest.getEmail());
            return "redirect:/auth/verify-email";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.registration", e.getMessage());
            logger.warn("Registration failed: {}", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            model.addAttribute("error", "An unexpected error occurred. Please try again.");
            return "auth/register";
        }
    }

    /**
     * Show email verification form
     */
    @GetMapping("/verify-email")
    public String showVerificationForm(
            @RequestParam(required = false) String email,
            Model model) {
        VerificationRequest verificationRequest = new VerificationRequest();
        if (email != null && !email.isEmpty()) {
            verificationRequest.setEmail(email);
        }
        model.addAttribute("verificationRequest", verificationRequest);
        return "auth/verify-email";
    }

    /**
     * Handle email verification
     */
    @PostMapping("/verify-email")
    public String verifyEmail(
            @Valid @ModelAttribute VerificationRequest verificationRequest,
            BindingResult bindingResult,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/verify-email";
        }

        try {
            // Create registration request for user creation
            RegistrationRequest originalRequest = new RegistrationRequest(
                username,
                verificationRequest.getEmail(),
                password,
                passwordConfirm
            );

            registrationService.verifyEmailAndCreateUser(
                verificationRequest.getEmail(),
                verificationRequest.getCode(),
                originalRequest
            );

            redirectAttributes.addFlashAttribute("success", 
                "Email verified! Your account has been created. You can now login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("code", "error.verification", e.getMessage());
            logger.warn("Verification failed: {}", e.getMessage());
            return "auth/verify-email";
        } catch (Exception e) {
            logger.error("Unexpected error during verification", e);
            model.addAttribute("error", "An unexpected error occurred. Please try again.");
            return "auth/verify-email";
        }
    }

    /**
     * Resend verification code
     */
    @PostMapping("/resend-code")
    public String resendCode(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        try {
            registrationService.resendVerificationCode(email);
            redirectAttributes.addFlashAttribute("message", 
                "Verification code resent to " + email);
            return "redirect:/auth/verify-email?email=" + email;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/verify-email";
        } catch (Exception e) {
            logger.error("Error resending verification code", e);
            redirectAttributes.addFlashAttribute("error", "Failed to resend code. Please try again.");
            return "redirect:/auth/verify-email";
        }
    }
}
