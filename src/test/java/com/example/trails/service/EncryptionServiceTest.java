package com.example.trails.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EncryptionService
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Encryption Service Tests")
class EncryptionServiceTest {

    @Autowired
    private EncryptionService encryptionService;

    @Test
    @DisplayName("Encrypt and decrypt text successfully")
    void testEncryptAndDecrypt() throws Exception {
        String plaintext = "sensitive_password_123";
        
        String encrypted = encryptionService.encrypt(plaintext);
        assertNotNull(encrypted, "Encrypted text should not be null");
        assertNotEquals(plaintext, encrypted, "Encrypted text should differ from plaintext");
        
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plaintext, decrypted, "Decrypted text should match original");
    }

    @Test
    @DisplayName("Handle null plaintext")
    void testEncryptNull() throws Exception {
        String encrypted = encryptionService.encrypt(null);
        assertNull(encrypted, "Null plaintext should return null");
    }

    @Test
    @DisplayName("Handle empty plaintext")
    void testEncryptEmpty() throws Exception {
        String encrypted = encryptionService.encrypt("");
        assertEquals("", encrypted, "Empty plaintext should return empty");
    }

    @Test
    @DisplayName("Handle whitespace plaintext")
    void testEncryptWhitespace() throws Exception {
        String encrypted = encryptionService.encrypt("   ");
        assertEquals("   ", encrypted, "Whitespace-only plaintext should return as-is");
    }

    @Test
    @DisplayName("Decrypt null returns null")
    void testDecryptNull() throws Exception {
        String decrypted = encryptionService.decrypt(null);
        assertNull(decrypted, "Null ciphertext should return null");
    }

    @Test
    @DisplayName("Decrypt empty returns empty")
    void testDecryptEmpty() throws Exception {
        String decrypted = encryptionService.decrypt("");
        assertEquals("", decrypted, "Empty ciphertext should return empty");
    }

    @Test
    @DisplayName("Encrypt and decrypt special characters")
    void testEncryptSpecialCharacters() throws Exception {
        String plaintext = "user@example.com!@#$%^&*()";
        
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plaintext, decrypted, "Special characters should be preserved");
    }

    @Test
    @DisplayName("Encrypt and decrypt long text")
    void testEncryptLongText() throws Exception {
        String plaintext = "This is a very long text that contains multiple sentences. " +
                "It should be encrypted and decrypted correctly. " +
                "Even with newlines\nand\ttabs it should work.";
        
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plaintext, decrypted, "Long text with special chars should work");
    }

    @Test
    @DisplayName("Same plaintext produces different ciphertext (non-deterministic)")
    void testEncryptionNonDeterministic() throws Exception {
        String plaintext = "test_password";
        
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        
        // Both should decrypt to the same value
        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Encrypt unicode characters")
    void testEncryptUnicode() throws Exception {
        String plaintext = "Müller@ëxämplé.com 日本語 🔐";
        
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plaintext, decrypted, "Unicode characters should be preserved");
    }
}
