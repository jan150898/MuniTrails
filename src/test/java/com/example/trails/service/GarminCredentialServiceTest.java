package com.example.trails.service;

import com.example.trails.model.User;
import com.example.trails.repo.GarminCredentialRepository;
import com.example.trails.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GarminCredentialService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Garmin Credential Service Tests")
class GarminCredentialServiceTest {

    @Autowired
    private GarminCredentialService garminCredentialService;

    @Autowired
    private GarminCredentialRepository garminCredentialRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        garminCredentialRepository.deleteAll();
        userRepository.deleteAll();
        
        testUser = new User();
        testUser.setUsername("test_user");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser.setRole("ROLE_USER");
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Save credentials successfully")
    void testSaveCredentialsSuccessfully() throws Exception {
        String email = "user@garmin.com";
        String password = "garmin_password";

        var saved = garminCredentialService.saveCredentials(testUser, email, password);

        assertNotNull(saved.getId(), "Saved credential should have ID");
        assertNotNull(saved.getEncryptedEmail(), "Email should be encrypted");
        assertNotNull(saved.getEncryptedPassword(), "Password should be encrypted");
        assertEquals(testUser.getId(), saved.getUser().getId(), "User should be set");
    }

    @Test
    @DisplayName("Retrieve and decrypt credentials")
    void testGetCredentials() throws Exception {
        String email = "user@garmin.com";
        String password = "garmin_password";

        garminCredentialService.saveCredentials(testUser, email, password);

        Optional<GarminCredentialService.GarminCredentials> retrieved = 
            garminCredentialService.getCredentials(testUser);

        assertTrue(retrieved.isPresent(), "Credentials should be retrieved");
        assertEquals(email, retrieved.get().getEmail(), "Email should be decrypted correctly");
        assertEquals(password, retrieved.get().getPassword(), "Password should be decrypted correctly");
    }

    @Test
    @DisplayName("Check credentials exist")
    void testHasCredentials() throws Exception {
        assertFalse(garminCredentialService.hasCredentials(testUser), "No credentials initially");

        garminCredentialService.saveCredentials(testUser, "email@test.com", "password");

        assertTrue(garminCredentialService.hasCredentials(testUser), "Credentials should exist");
    }

    @Test
    @DisplayName("Delete credentials")
    void testDeleteCredentials() throws Exception {
        garminCredentialService.saveCredentials(testUser, "email@test.com", "password");
        assertTrue(garminCredentialService.hasCredentials(testUser), "Credentials should exist");

        garminCredentialService.deleteCredentials(testUser);

        assertFalse(garminCredentialService.hasCredentials(testUser), "Credentials should be deleted");
    }

    @Test
    @DisplayName("Update credentials overwrites previous")
    void testUpdateCredentials() throws Exception {
        garminCredentialService.saveCredentials(testUser, "old@garmin.com", "old_password");

        String newEmail = "new@garmin.com";
        String newPassword = "new_password";
        garminCredentialService.saveCredentials(testUser, newEmail, newPassword);

        Optional<GarminCredentialService.GarminCredentials> retrieved = 
            garminCredentialService.getCredentials(testUser);

        assertTrue(retrieved.isPresent(), "Updated credentials should exist");
        assertEquals(newEmail, retrieved.get().getEmail(), "Email should be updated");
        assertEquals(newPassword, retrieved.get().getPassword(), "Password should be updated");
    }

    @Test
    @DisplayName("Throw exception on missing email")
    void testSaveCredentialsWithoutEmail() {
        assertThrows(IllegalArgumentException.class, () -> {
            garminCredentialService.saveCredentials(testUser, null, "password");
        }, "Should throw exception for null email");
    }

    @Test
    @DisplayName("Throw exception on missing password")
    void testSaveCredentialsWithoutPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            garminCredentialService.saveCredentials(testUser, "email@test.com", null);
        }, "Should throw exception for null password");
    }

    @Test
    @DisplayName("Throw exception on empty email")
    void testSaveCredentialsWithEmptyEmail() {
        assertThrows(IllegalArgumentException.class, () -> {
            garminCredentialService.saveCredentials(testUser, "", "password");
        }, "Should throw exception for empty email");
    }

    @Test
    @DisplayName("Throw exception on empty password")
    void testSaveCredentialsWithEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            garminCredentialService.saveCredentials(testUser, "email@test.com", "");
        }, "Should throw exception for empty password");
    }

    @Test
    @DisplayName("Get credentials returns empty for user without credentials")
    void testGetCredentialsNotFound() throws Exception {
        Optional<GarminCredentialService.GarminCredentials> retrieved = 
            garminCredentialService.getCredentials(testUser);

        assertTrue(retrieved.isEmpty(), "Should return empty optional");
    }

    @Test
    @DisplayName("Handle special characters in email and password")
    void testSpecialCharactersInCredentials() throws Exception {
        String email = "user+special@domain.co.uk";
        String password = "P@ssw0rd!@#$%^&*()";

        garminCredentialService.saveCredentials(testUser, email, password);

        Optional<GarminCredentialService.GarminCredentials> retrieved = 
            garminCredentialService.getCredentials(testUser);

        assertTrue(retrieved.isPresent(), "Credentials with special chars should be saved");
        assertEquals(email, retrieved.get().getEmail(), "Special chars in email preserved");
        assertEquals(password, retrieved.get().getPassword(), "Special chars in password preserved");
    }
}
