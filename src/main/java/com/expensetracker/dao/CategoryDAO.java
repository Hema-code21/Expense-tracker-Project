package com.expensetracker.dao;

import com.expensetracker.model.Category;
import com.expensetracker.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Category} entities.
 *
 * <p>Categories are predefined and shared across all users. This DAO is
 * read-only in normal application flow; seed data is inserted by
 * {@code schema.sql}.</p>
 */
public class CategoryDAO {

    private static final String FIND_ALL    = "SELECT id, name FROM categories ORDER BY name";
    private static final String FIND_BY_ID  = "SELECT id, name FROM categories WHERE id = ?";
    private static final String FIND_BY_NAME = "SELECT id, name FROM categories WHERE name = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns all categories ordered alphabetically.
     *
     * @throws SQLException if a database error occurs
     */
    public List<Category> findAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(mapRow(rs));
            }
        }
        return categories;
    }

    /**
     * Finds a category by its primary key.
     *
     * @param id the category id
     * @return an {@link Optional} containing the category, or empty if not found
     * @throws SQLException if a database error occurs
     */
    public Optional<Category> findById(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a category by name (case-sensitive).
     *
     * @param name the category name
     * @return an {@link Optional} containing the category, or empty if not found
     * @throws SQLException if a database error occurs
     */
    public Optional<Category> findByName(String name) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_NAME)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(rs.getInt("id"), rs.getString("name"));
    }
}
