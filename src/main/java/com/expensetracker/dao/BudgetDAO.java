package com.expensetracker.dao;

import com.expensetracker.model.Budget;
import com.expensetracker.util.DBConnection;

import java.sql.*;
import java.util.Optional;

/**
 * Data Access Object for {@link Budget} entities.
 *
 * <p>The unique constraint {@code (user_id, month, year)} is enforced at the
 * database level.  The {@link #saveOrUpdate} method uses MySQL's
 * {@code INSERT … ON DUPLICATE KEY UPDATE} idiom to upsert a budget.</p>
 */
public class BudgetDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------

    /**
     * Upsert: insert a new budget or update the amount if one already exists
     * for this user/month/year combination.
     */
    private static final String UPSERT =
            "INSERT INTO budgets (user_id, month, year, amount) VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE amount = VALUES(amount)";

    private static final String FIND_BY_USER_MONTH_YEAR =
            "SELECT id, user_id, month, year, amount, created_at, updated_at " +
            "FROM budgets WHERE user_id = ? AND month = ? AND year = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Inserts or updates a monthly budget for a user.
     *
     * @param budget the budget to save (userId, month, year, and amount must be set)
     * @throws SQLException if a database error occurs
     */
    public void saveOrUpdate(Budget budget) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT)) {

            ps.setInt(1, budget.getUserId());
            ps.setInt(2, budget.getMonth());
            ps.setInt(3, budget.getYear());
            ps.setBigDecimal(4, budget.getAmount());
            ps.executeUpdate();
        }
    }

    /**
     * Finds the budget for a specific user, month, and year.
     *
     * @param userId the owning user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @return an {@link Optional} containing the budget, or empty if none is set
     * @throws SQLException if a database error occurs
     */
    public Optional<Budget> findByUserMonthYear(int userId, int month, int year) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_USER_MONTH_YEAR)) {

            ps.setInt(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
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

    private Budget mapRow(ResultSet rs) throws SQLException {
        Budget budget = new Budget();
        budget.setId(rs.getInt("id"));
        budget.setUserId(rs.getInt("user_id"));
        budget.setMonth(rs.getInt("month"));
        budget.setYear(rs.getInt("year"));
        budget.setAmount(rs.getBigDecimal("amount"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) budget.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) budget.setUpdatedAt(updated.toLocalDateTime());

        return budget;
    }
}
