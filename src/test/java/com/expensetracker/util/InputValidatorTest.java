package com.expensetracker.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InputValidator}.
 *
 * <p>No database connection is needed. Each test exercises validation logic
 * in isolation.</p>
 */
@DisplayName("InputValidator Tests")
class InputValidatorTest {

    // ─── validateUsername ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid username passes validation")
    void testValidUsername() {
        List<String> errors = InputValidator.validateUsername("alice_99");
        assertTrue(errors.isEmpty(), "Expected no errors for valid username");
    }

    @Test
    @DisplayName("Null username fails validation")
    void testNullUsername() {
        assertFalse(InputValidator.validateUsername(null).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "this_username_is_way_too_long_exceeding_fifty_chars_limit_xyz"})
    @DisplayName("Username outside 3-50 char range fails")
    void testUsernameLengthBounds(String username) {
        assertFalse(InputValidator.validateUsername(username).isEmpty());
    }

    @Test
    @DisplayName("Username with special characters fails")
    void testUsernameSpecialChars() {
        assertFalse(InputValidator.validateUsername("alice@domain").isEmpty());
    }

    // ─── validateEmail ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid email passes validation")
    void testValidEmail() {
        assertTrue(InputValidator.validateEmail("user@example.com").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"notanemail", "missing@dot", "@nodomain.com", ""})
    @DisplayName("Invalid email formats fail validation")
    void testInvalidEmails(String email) {
        assertFalse(InputValidator.validateEmail(email).isEmpty());
    }

    // ─── validatePassword ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid password passes validation")
    void testValidPassword() {
        assertTrue(InputValidator.validatePassword("Secure123").isEmpty());
    }

    @Test
    @DisplayName("Password shorter than 8 chars fails")
    void testPasswordTooShort() {
        assertFalse(InputValidator.validatePassword("Pass1").isEmpty());
    }

    @Test
    @DisplayName("Password without a digit fails")
    void testPasswordNoDigit() {
        assertFalse(InputValidator.validatePassword("PasswordOnly").isEmpty());
    }

    @Test
    @DisplayName("Password without a letter fails")
    void testPasswordNoLetter() {
        assertFalse(InputValidator.validatePassword("12345678").isEmpty());
    }

    // ─── validateAmount ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid amount passes validation")
    void testValidAmount() {
        assertTrue(InputValidator.validateAmount("1500.00").isEmpty());
        assertTrue(InputValidator.validateAmount("0.01").isEmpty());
        assertTrue(InputValidator.validateAmount("999").isEmpty());
    }

    @Test
    @DisplayName("Zero amount fails validation")
    void testZeroAmount() {
        assertFalse(InputValidator.validateAmount("0").isEmpty());
        assertFalse(InputValidator.validateAmount("0.00").isEmpty());
    }

    @Test
    @DisplayName("Negative amount fails validation")
    void testNegativeAmount() {
        assertFalse(InputValidator.validateAmount("-100").isEmpty());
    }

    @Test
    @DisplayName("Amount with more than 2 decimal places fails")
    void testAmountTooManyDecimals() {
        assertFalse(InputValidator.validateAmount("10.123").isEmpty());
    }

    @Test
    @DisplayName("Non-numeric amount fails validation")
    void testNonNumericAmount() {
        assertFalse(InputValidator.validateAmount("abc").isEmpty());
    }

    // ─── validateDate ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid ISO date passes validation")
    void testValidDate() {
        assertTrue(InputValidator.validateDate("2024-03-15").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"15-03-2024", "2024/03/15", "20240315", "not-a-date"})
    @DisplayName("Non-ISO date formats fail validation")
    void testInvalidDateFormats(String date) {
        assertFalse(InputValidator.validateDate(date).isEmpty());
    }

    @Test
    @DisplayName("Blank date fails validation")
    void testBlankDate() {
        assertFalse(InputValidator.validateDate("").isEmpty());
        assertFalse(InputValidator.validateDate(null).isEmpty());
    }

    // ─── validateMonth ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid months (1-12) pass validation")
    void testValidMonths() {
        for (int m = 1; m <= 12; m++) {
            assertTrue(InputValidator.validateMonth(m).isEmpty(),
                    "Month " + m + " should be valid");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13, -1, 100})
    @DisplayName("Invalid month values fail validation")
    void testInvalidMonths(int month) {
        assertFalse(InputValidator.validateMonth(month).isEmpty());
    }

    // ─── parseIntSafe ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("parseIntSafe returns parsed value for valid int string")
    void testParseIntSafe_valid() {
        assertEquals(42, InputValidator.parseIntSafe("42", -1));
    }

    @Test
    @DisplayName("parseIntSafe returns default for invalid string")
    void testParseIntSafe_invalid() {
        assertEquals(-1, InputValidator.parseIntSafe("xyz", -1));
    }

    @Test
    @DisplayName("parseIntSafe returns default for null")
    void testParseIntSafe_null() {
        assertEquals(0, InputValidator.parseIntSafe(null, 0));
    }

    // ─── validateDescription ──────────────────────────────────────────────────

    @Test
    @DisplayName("Valid description passes")
    void testValidDescription() {
        assertTrue(InputValidator.validateDescription("Grocery shopping at Big Bazaar").isEmpty());
    }

    @Test
    @DisplayName("Blank description fails")
    void testBlankDescription() {
        assertFalse(InputValidator.validateDescription("   ").isEmpty());
    }

    @Test
    @DisplayName("Description exceeding 255 chars fails")
    void testDescriptionTooLong() {
        String longDesc = "a".repeat(256);
        assertFalse(InputValidator.validateDescription(longDesc).isEmpty());
    }
}
