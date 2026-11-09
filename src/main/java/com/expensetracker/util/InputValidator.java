package com.expensetracker.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates user input before it reaches the service or DAO layer.
 *
 * <p>Each method returns a (possibly empty) list of human-readable error
 * messages. An empty list means the input is valid.</p>
 *
 * <p>Example:
 * <pre>{@code
 * List<String> errors = InputValidator.validateUsername("alice");
 * if (!errors.isEmpty()) {
 *     errors.forEach(System.out::println);
 * }
 * }</pre>
 * </p>
 */
public final class InputValidator {

    // Prevent instantiation
    private InputValidator() {}

    // -------------------------------------------------------------------------
    // User validation
    // -------------------------------------------------------------------------

    /**
     * Validates a username.
     *
     * <p>Rules: 3-50 characters, alphanumeric and underscores only.</p>
     */
    public static List<String> validateUsername(String username) {
        List<String> errors = new ArrayList<>();
        if (username == null || username.isBlank()) {
            errors.add("Username must not be blank.");
            return errors;
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3 || trimmed.length() > 50) {
            errors.add("Username must be between 3 and 50 characters.");
        }
        if (!trimmed.matches("[a-zA-Z0-9_]+")) {
            errors.add("Username may only contain letters, digits, and underscores.");
        }
        return errors;
    }

    /**
     * Validates an email address (basic format check).
     */
    public static List<String> validateEmail(String email) {
        List<String> errors = new ArrayList<>();
        if (email == null || email.isBlank()) {
            errors.add("Email must not be blank.");
            return errors;
        }
        String trimmed = email.trim();
        if (trimmed.length() > 150) {
            errors.add("Email must not exceed 150 characters.");
        }
        // Simple regex: something@something.something
        if (!trimmed.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.add("Email address is not valid.");
        }
        return errors;
    }

    /**
     * Validates a plain-text password.
     *
     * <p>Rules: at least 8 characters, at least one digit, and at least one
     * letter.</p>
     */
    public static List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.isBlank()) {
            errors.add("Password must not be blank.");
            return errors;
        }
        if (password.length() < 8) {
            errors.add("Password must be at least 8 characters long.");
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            errors.add("Password must contain at least one letter.");
        }
        if (!password.matches(".*\\d.*")) {
            errors.add("Password must contain at least one digit.");
        }
        return errors;
    }

    // -------------------------------------------------------------------------
    // Transaction validation
    // -------------------------------------------------------------------------

    /**
     * Validates a monetary amount string (e.g., "1500.50").
     *
     * <p>Must be a positive number with at most 2 decimal places and not
     * exceed 9,999,999,999.99.</p>
     */
    public static List<String> validateAmount(String amountStr) {
        List<String> errors = new ArrayList<>();
        if (amountStr == null || amountStr.isBlank()) {
            errors.add("Amount must not be blank.");
            return errors;
        }
        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Amount must be greater than zero.");
            }
            if (amount.scale() > 2) {
                errors.add("Amount must have at most 2 decimal places.");
            }
            if (amount.compareTo(new BigDecimal("9999999999.99")) > 0) {
                errors.add("Amount is too large.");
            }
        } catch (NumberFormatException e) {
            errors.add("Amount must be a valid number (e.g., 150 or 1500.50).");
        }
        return errors;
    }

    /**
     * Validates a description string.
     *
     * <p>Must not be blank and must not exceed 255 characters.</p>
     */
    public static List<String> validateDescription(String description) {
        List<String> errors = new ArrayList<>();
        if (description == null || description.isBlank()) {
            errors.add("Description must not be blank.");
            return errors;
        }
        if (description.trim().length() > 255) {
            errors.add("Description must not exceed 255 characters.");
        }
        return errors;
    }

    /**
     * Validates a date string in ISO format ({@code yyyy-MM-dd}).
     *
     * <p>Also rejects dates that are more than 10 years in the future.</p>
     */
    public static List<String> validateDate(String dateStr) {
        List<String> errors = new ArrayList<>();
        if (dateStr == null || dateStr.isBlank()) {
            errors.add("Date must not be blank.");
            return errors;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            if (date.isAfter(LocalDate.now().plusYears(10))) {
                errors.add("Date is too far in the future.");
            }
        } catch (DateTimeParseException e) {
            errors.add("Date must be in yyyy-MM-dd format (e.g., 2024-03-15).");
        }
        return errors;
    }

    // -------------------------------------------------------------------------
    // Budget validation
    // -------------------------------------------------------------------------

    /**
     * Validates a month value.
     *
     * @param month integer representing a calendar month
     */
    public static List<String> validateMonth(int month) {
        List<String> errors = new ArrayList<>();
        if (month < 1 || month > 12) {
            errors.add("Month must be between 1 and 12.");
        }
        return errors;
    }

    /**
     * Validates a year value.
     *
     * @param year four-digit calendar year
     */
    public static List<String> validateYear(int year) {
        List<String> errors = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        if (year < 2000 || year > currentYear + 10) {
            errors.add("Year must be between 2000 and " + (currentYear + 10) + ".");
        }
        return errors;
    }

    // -------------------------------------------------------------------------
    // Generic helpers
    // -------------------------------------------------------------------------

    /**
     * Safely parses a string as an integer, returning {@code defaultValue}
     * if parsing fails.
     */
    public static int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns {@code true} if a string represents a valid positive integer.
     */
    public static boolean isPositiveInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
