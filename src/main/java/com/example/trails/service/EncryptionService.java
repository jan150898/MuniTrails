package com.example.trails.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    @Value("${app.encryption.key:}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    /**
     * Encrypts a plaintext string using AES encryption.
     */
    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        
        SecretKey secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypts a Base64-encoded encrypted string.
     */
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }
        
        SecretKey secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes);
    }

    /**
     * Gets or generates the secret key for encryption/decryption.
     * In production, use an external key management system (KMS).
     */
    private SecretKey getSecretKey() throws Exception {
        if (encryptionKey != null && !encryptionKey.isBlank()) {
            // Decode the hex-encoded key from properties
            byte[] decodedKey = hexStringToByteArray(encryptionKey);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
        }
        
        // Fallback: generate a new key (NOT suitable for production)
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey();
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
