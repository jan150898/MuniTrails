package com.example.trails.service;

import com.example.trails.model.GarminCredential;
import com.example.trails.model.User;
import com.example.trails.repo.GarminCredentialRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GarminCredentialService {

    private final GarminCredentialRepository credentialRepository;
    private final EncryptionService encryptionService;

    public GarminCredentialService(GarminCredentialRepository credentialRepository,
                                   EncryptionService encryptionService) {
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Saves or updates Garmin credentials for a user (encrypted).
     */
    public GarminCredential saveCredentials(User user, String email, String password) throws Exception {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        // Find existing credential or create new one
        Optional<GarminCredential> existing = credentialRepository.findByUser(user);
        GarminCredential credential = existing.orElseGet(GarminCredential::new);

        credential.setUser(user);
        credential.setEncryptedEmail(encryptionService.encrypt(email));
        credential.setEncryptedPassword(encryptionService.encrypt(password));

        return credentialRepository.save(credential);
    }

    /**
     * Retrieves credentials for a user (decrypted).
     */
    public Optional<GarminCredentials> getCredentials(User user) throws Exception {
        Optional<GarminCredential> credential = credentialRepository.findByUser(user);
        if (credential.isEmpty()) {
            return Optional.empty();
        }

        GarminCredential gc = credential.get();
        String email = encryptionService.decrypt(gc.getEncryptedEmail());
        String password = encryptionService.decrypt(gc.getEncryptedPassword());
        if (encryptionService.isLegacyCiphertext(gc.getEncryptedEmail()) || encryptionService.isLegacyCiphertext(gc.getEncryptedPassword())) {
            gc.setEncryptedEmail(encryptionService.encrypt(email));
            gc.setEncryptedPassword(encryptionService.encrypt(password));
            credentialRepository.save(gc);
        }

        return Optional.of(new GarminCredentials(email, password));
    }

    /**
     * Deletes credentials for a user.
     */
    public void deleteCredentials(User user) {
        credentialRepository.deleteByUser(user);
    }

    /**
     * Checks if a user has saved credentials.
     */
    public boolean hasCredentials(User user) {
        return credentialRepository.findByUser(user).isPresent();
    }

    /**
     * DTO for returning decrypted credentials (without exposing the model).
     */
    public static class GarminCredentials {
        private final String email;
        private final String password;

        public GarminCredentials(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }
    }
}
