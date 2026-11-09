package com.expensetracker.dao;

import com.expensetracker.model.User;
import com.expensetracker.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data Access Object for {@link User} entities.
 *
 * <p>All SQL queries use {@link PreparedStatement} to prevent SQL injection.
 * All JDBC resources are managed with try-with-resources.</p>
 */
public class UserDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------

    private static final String INSERT_USER =
            "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";

    private static final String FIND_BY_USERNAME =
            "SELECT id, username, email, password, created_at FROM users WHERE username = ?";

    private static final String FIND_BY_EMAIL =
            "SELECT id, username, email, password, created_at FROM users WHERE email = ?";

    private static final String EXISTS_BY_USERNAME =
            "SELECT COUNT(1) FROM users WHERE username = ?";

    private static final String EXISTS_BY_EMAIL =
            "SELECT COUNT(1) FROM users WHERE email = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Inserts a new user into the database.
     *
     * @param user user to insert (password must already be hashed)
     * @return the generated user id
     * @throws SQLException if a database error occurs or a unique constraint is violated
     */
    public int save(User user) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_USER,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Creating user failed — no ID obtained.");
    }

    /**
     * Finds a user by username.
     *
     * @param username the username to look up
     * @return an {@link Optional} containing the user, or empty if not found
     * @throws SQLException if a database error occurs
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_USERNAME)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a user by email address.
     *
     * @param email the email to look up
     * @return an {@link Optional} containing the user, or empty if not found
     * @throws SQLException if a database error occurs
     */
    public Optional<User> findByEmail(String email) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_EMAIL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns {@code true} if a user with the given username already exists.
     *
     * @param username the username to check
     * @throws SQLException if a database error occurs
     */
    public boolean existsByUsername(String username) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(EXISTS_BY_USERNAME)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Returns {@code true} if a user with the given email already exists.
     *
     * @param email the email to check
     * @throws SQLException if a database error occurs
     */
    public boolean existsByEmail(String email) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(EXISTS_BY_EMAIL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Maps a ResultSet row to a {@link User} object. */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }
}
