package com.expensetracker.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility class for BCrypt password hashing and verification.
 *
 * <p>BCrypt is a deliberately slow hashing algorithm that makes brute-force
 * attacks computationally expensive. A work factor (cost) of 12 is used,
 * which strikes a good balance between security and response time.</p>
 *
 * <p>Usage:
 * <pre>{@code
 * // Hash a plain-text password before storing
 * String hash = PasswordUtil.hashPassword("mySecret");
 *
 * // Verify a login attempt
 * boolean valid = PasswordUtil.verifyPassword("mySecret", hash);
 * }</pre>
 * </p>
 */
public final class PasswordUtil {

    /** BCrypt work factor — higher means slower (and safer). */
    private static final int COST = 12;

    // Prevent instantiation
    private PasswordUtil() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * @param plainTextPassword the password to hash (must not be null or empty)
     * @return a BCrypt hash string safe to store in the database
     * @throws IllegalArgumentException if the password is null or blank
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or blank.");
        }
        return BCrypt.withDefaults().hashToString(COST, plainTextPassword.toCharArray());
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * @param plainTextPassword the candidate password
     * @param hashedPassword    the stored BCrypt hash
     * @return {@code true} if the password matches the hash
     */
    public static boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(
                plainTextPassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}
