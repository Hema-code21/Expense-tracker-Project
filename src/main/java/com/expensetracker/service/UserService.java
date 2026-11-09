package com.expensetracker.service;

import com.expensetracker.dao.UserDAO;
import com.expensetracker.model.User;
import com.expensetracker.util.InputValidator;
import com.expensetracker.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for user registration and authentication.
 *
 * <p>This service coordinates between {@link UserDAO} and the utility classes
 * so that the UI layer ({@code MenuHandler}) only needs to call high-level
 * methods and receive clear error messages.</p>
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /** Constructor for dependency injection (useful in tests). */
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new user.
     *
     * <p>Steps:
     * <ol>
     *   <li>Validates all input fields.</li>
     *   <li>Checks that the username and email are not already taken.</li>
     *   <li>Hashes the password with BCrypt.</li>
     *   <li>Persists the user.</li>
     * </ol>
     * </p>
     *
     * @param username plain-text username
     * @param email    email address
     * @param password plain-text password (will be hashed before storage)
     * @return the saved {@link User} (with generated id)
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if a database error occurs
     */
    public User register(String username, String email, String password) {
        // --- Validate input ---
        List<String> errors = new java.util.ArrayList<>();
        errors.addAll(InputValidator.validateUsername(username));
        errors.addAll(InputValidator.validateEmail(email));
        errors.addAll(InputValidator.validatePassword(password));

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        try {
            // --- Check uniqueness ---
            if (userDAO.existsByUsername(username.trim())) {
                throw new IllegalArgumentException("Username '" + username.trim() + "' is already taken.");
            }
            if (userDAO.existsByEmail(email.trim())) {
                throw new IllegalArgumentException("Email '" + email.trim() + "' is already registered.");
            }

            // --- Hash password and save ---
            String hashedPassword = PasswordUtil.hashPassword(password);
            User user = new User(username.trim(), email.trim().toLowerCase(), hashedPassword);
            int id = userDAO.save(user);
            user.setId(id);
            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during registration: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user by username and password.
     *
     * @param username the username to look up
     * @param password the plain-text password to verify
     * @return the authenticated {@link User}
     * @throws IllegalArgumentException if credentials are invalid
     * @throws RuntimeException         if a database error occurs
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank.");
        }

        try {
            Optional<User> userOpt = userDAO.findByUsername(username.trim());
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid username or password.");
            }

            User user = userOpt.get();
            if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
                throw new IllegalArgumentException("Invalid username or password.");
            }

            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during login: " + e.getMessage(), e);
        }
    }
}
