package com.example.trails.util;

import java.util.regex.Pattern;

/**
 * Minimal password complexity policy: at least 8 characters, one letter
 * and one digit. Intentionally simple so it doesn't punish real users
 * with an overly strict ruleset, while still ruling out trivially weak
 * passwords like "12345678" or "password".
 */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final Pattern HAS_LETTER = Pattern.compile("[a-zA-Z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");

    private PasswordPolicy() {}

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (!HAS_LETTER.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one letter");
        }
        if (!HAS_DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
    }
}
