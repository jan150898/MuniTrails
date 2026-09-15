package com.example.trails.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for database seeding.
 * Run main() to generate hashes for new credentials.
 */
public class PasswordEncoderUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (args.length == 0) {
            System.err.println("Usage: PasswordEncoderUtil <password> [<password> ...]");
            System.exit(1);
        }
        for (String password : args) {
            System.out.println(encoder.encode(password));
        }
    }
}
