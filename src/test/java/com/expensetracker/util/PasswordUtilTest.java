package com.expensetracker.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PasswordUtil}.
 *
 * <p>These tests verify BCrypt hashing and verification logic without any
 * database dependency.</p>
 */
@DisplayName("PasswordUtil Tests")
class PasswordUtilTest {

    // ─── hashPassword ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("hashPassword returns a non-null BCrypt hash")
    void testHashPassword_returnsNonNull() {
        String hash = PasswordUtil.hashPassword("SecurePass1");
        assertNotNull(hash, "Hash should not be null");
    }

    @Test
    @DisplayName("hashPassword produces a different hash each call (salt randomness)")
    void testHashPassword_differentSalts() {
        String hash1 = PasswordUtil.hashPassword("SecurePass1");
        String hash2 = PasswordUtil.hashPassword("SecurePass1");
        // BCrypt embeds a random salt, so two hashes of the same input differ
        assertNotEquals(hash1, hash2, "BCrypt hashes should differ due to random salt");
    }

    @Test
    @DisplayName("hashPassword throws on null input")
    void testHashPassword_nullInput_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.hashPassword(null));
    }

    @Test
    @DisplayName("hashPassword throws on blank input")
    void testHashPassword_blankInput_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.hashPassword("   "));
    }

    // ─── verifyPassword ───────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyPassword returns true for correct password")
    void testVerifyPassword_correct() {
        String plain = "MyPassword9";
        String hash  = PasswordUtil.hashPassword(plain);
        assertTrue(PasswordUtil.verifyPassword(plain, hash),
                "Correct password should verify successfully");
    }

    @Test
    @DisplayName("verifyPassword returns false for wrong password")
    void testVerifyPassword_wrong() {
        String hash = PasswordUtil.hashPassword("RightPass1");
        assertFalse(PasswordUtil.verifyPassword("WrongPass1", hash),
                "Wrong password should not verify");
    }

    @Test
    @DisplayName("verifyPassword returns false when plain password is null")
    void testVerifyPassword_nullPlain() {
        String hash = PasswordUtil.hashPassword("ValidPass1");
        assertFalse(PasswordUtil.verifyPassword(null, hash));
    }

    @Test
    @DisplayName("verifyPassword returns false when hash is null")
    void testVerifyPassword_nullHash() {
        assertFalse(PasswordUtil.verifyPassword("ValidPass1", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "PASSWORD1", "pass1", "alllower7"})
    @DisplayName("verifyPassword is case-sensitive")
    void testVerifyPassword_caseSensitive(String candidate) {
        String original = "Password1";
        String hash     = PasswordUtil.hashPassword(original);
        if (!candidate.equals(original)) {
            assertFalse(PasswordUtil.verifyPassword(candidate, hash),
                    "Different case should not verify as the same password");
        }
    }
}
