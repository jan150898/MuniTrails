package com.example.trails.web;

import com.example.trails.model.User;
import com.example.trails.service.GarminCredentialService;
import com.example.trails.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile/garmin")
public class GarminCredentialController {

    private final GarminCredentialService garminCredentialService;
    private final UserService userService;

    public GarminCredentialController(GarminCredentialService garminCredentialService,
                                     UserService userService) {
        this.garminCredentialService = garminCredentialService;
        this.userService = userService;
    }

    /**
     * Save or update Garmin credentials.
     * POST /api/v1/profile/garmin/save
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveCredentials(
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            String email = body.get("email");
            String password = body.get("password");

            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required."));
            }

            User user = userService.getUserByUsername(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found."));
            }

            garminCredentialService.saveCredentials(user, email, password);
            return ResponseEntity.ok(Map.of("status", "Credentials saved securely."));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not save credentials: " + e.getMessage()));
        }
    }

    /**
     * Check if user has saved credentials (returns masked email).
     * GET /api/v1/profile/garmin/has-credentials
     */
    @GetMapping("/has-credentials")
    public ResponseEntity<?> hasCredentials(Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (!garminCredentialService.hasCredentials(user)) {
                return ResponseEntity.ok(Map.of("hasCredentials", false));
            }

            var credentials = garminCredentialService.getCredentials(user);
            if (credentials.isEmpty()) {
                return ResponseEntity.ok(Map.of("hasCredentials", false));
            }

            String maskedEmail = maskEmail(credentials.get().getEmail());
            return ResponseEntity.ok(Map.of("hasCredentials", true, "email", maskedEmail));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not check credentials: " + e.getMessage()));
        }
    }

    /**
     * Delete Garmin credentials.
     * DELETE /api/v1/profile/garmin/delete
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCredentials(Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            garminCredentialService.deleteCredentials(user);
            return ResponseEntity.ok(Map.of("status", "Credentials deleted."));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not delete credentials: " + e.getMessage()));
        }
    }

    /**
     * Masks email for display (e.g., john***@example.com).
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return email.substring(0, 1) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
