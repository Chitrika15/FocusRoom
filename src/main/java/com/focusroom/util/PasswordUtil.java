package com.focusroom.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility for hashing and verifying student passwords using BCrypt.
 */
public class PasswordUtil {

    private static final int LOG_ROUNDS = 12;

    /**
     * Hashes a plain-text password using BCrypt with salt.
     *
     * @param plainPassword the plain text password
     * @return the bcrypt hashed password string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Verifies a plain-text password against a stored hashed password.
     *
     * @param plainPassword  the candidate plain text password
     * @param hashedPassword the hashed password from the database
     * @return true if the passwords match, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // In case of malformed hash
            return false;
        }
    }
}
