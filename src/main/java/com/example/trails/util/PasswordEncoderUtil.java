package com.example.trails.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for database seeding.
 * Run main() to generate hashes for new credentials.
 */
public class PasswordEncoderUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hashes for new users
        String janPassword = "1998Kati";
        String annaPassword = "MeinMuni";
        
        String janHash = encoder.encode(janPassword);
        String annaHash = encoder.encode(annaPassword);
        
        System.out.println("Jan / 1998Kati hash:");
        System.out.println(janHash);
        System.out.println();
        System.out.println("Anna / MeinMuni hash:");
        System.out.println(annaHash);
    }
}
