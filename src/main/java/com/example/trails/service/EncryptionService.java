package com.example.trails.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    @Value("${app.encryption.key:}")
    private String encryptionKey;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_NONCE_LENGTH = 12;
    private static final String CIPHERTEXT_VERSION = "v2:";

    private SecretKey secretKey;

    /**
     * Fail fast at startup if running in production without a configured
     * encryption key. Previously this service silently generated a brand
     * new random AES key on every single encrypt/decrypt call when no key
     * was configured, which meant Garmin credentials could never actually
     * be decrypted again once saved. Failing fast here surfaces the
     * misconfiguration immediately instead of causing silent data loss.
     */
    @PostConstruct
    public void init() {
        if (encryptionKey == null || !encryptionKey.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalStateException("app.encryption.key (APP_ENCRYPTION_KEY) must be a 64-character hexadecimal AES-256 key.");
        }
        byte[] decodedKey = hexStringToByteArray(encryptionKey);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    /**
     * Encrypts a plaintext string using AES encryption.
     */
    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[nonce.length + encryptedBytes.length];
        System.arraycopy(nonce, 0, payload, 0, nonce.length);
        System.arraycopy(encryptedBytes, 0, payload, nonce.length, encryptedBytes.length);
        return CIPHERTEXT_VERSION + Base64.getEncoder().encodeToString(payload);
    }

    /**
     * Decrypts a Base64-encoded encrypted string.
     */
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }
        
        if (!encryptedText.startsWith(CIPHERTEXT_VERSION)) {
            Cipher legacyCipher = Cipher.getInstance("AES");
            legacyCipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(legacyCipher.doFinal(Base64.getDecoder().decode(encryptedText)), StandardCharsets.UTF_8);
        }
        byte[] payload = Base64.getDecoder().decode(encryptedText.substring(CIPHERTEXT_VERSION.length()));
        if (payload.length <= GCM_NONCE_LENGTH) throw new IllegalArgumentException("Invalid encrypted credential");
        byte[] nonce = java.util.Arrays.copyOfRange(payload, 0, GCM_NONCE_LENGTH);
        byte[] ciphertext = java.util.Arrays.copyOfRange(payload, GCM_NONCE_LENGTH, payload.length);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    public boolean isLegacyCiphertext(String encryptedText) {
        return encryptedText != null && !encryptedText.isBlank() && !encryptedText.startsWith(CIPHERTEXT_VERSION);
    }

    /**
     * Converts a hex string to byte array.
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
