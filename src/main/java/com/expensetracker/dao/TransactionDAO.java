package com.expensetracker.dao;

import com.expensetracker.model.Transaction;
import com.expensetracker.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Transaction} entities.
 *
 * <p><strong>Security:</strong> Every query filters on {@code user_id} so that
 * users can never see or modify each other's data.  All queries use
 * {@link PreparedStatement}.</p>
 */
public class TransactionDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------

    private static final String BASE_SELECT =
            "SELECT t.id, t.user_id, t.category_id, c.name AS category_name, " +
            "       t.type, t.amount, t.description, t.transaction_date, " +
            "       t.created_at, t.updated_at " +
            "FROM transactions t " +
            "JOIN categories c ON t.category_id = c.id ";

    private static final String INSERT =
            "INSERT INTO transactions (user_id, category_id, type, amount, description, transaction_date) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE transactions " +
            "SET category_id = ?, type = ?, amount = ?, description = ?, transaction_date = ? " +
            "WHERE id = ? AND user_id = ?";

    private static final String DELETE =
            "DELETE FROM transactions WHERE id = ? AND user_id = ?";

    private static final String FIND_BY_ID =
            BASE_SELECT + "WHERE t.id = ? AND t.user_id = ?";

    private static final String FIND_ALL_BY_USER =
            BASE_SELECT + "WHERE t.user_id = ? ORDER BY t.transaction_date DESC, t.id DESC";

    private static final String FIND_BY_CATEGORY =
            BASE_SELECT + "WHERE t.user_id = ? AND t.category_id = ? " +
            "ORDER BY t.transaction_date DESC, t.id DESC";

    private static final String FIND_BY_DATE_RANGE =
            BASE_SELECT + "WHERE t.user_id = ? " +
            "AND t.transaction_date BETWEEN ? AND ? " +
            "ORDER BY t.transaction_date DESC, t.id DESC";

    private static final String SEARCH_BY_DESCRIPTION =
            BASE_SELECT + "WHERE t.user_id = ? AND t.description LIKE ? " +
            "ORDER BY t.transaction_date DESC, t.id DESC";

    private static final String FIND_BY_MONTH_YEAR =
            BASE_SELECT + "WHERE t.user_id = ? " +
            "AND MONTH(t.transaction_date) = ? AND YEAR(t.transaction_date) = ? " +
            "ORDER BY t.transaction_date DESC, t.id DESC";

    private static final String SUM_BY_TYPE_MONTH =
            "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE user_id = ? AND type = ? " +
            "AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ?";

    private static final String SUM_BY_CATEGORY_MONTH =
            "SELECT c.name, COALESCE(SUM(t.amount), 0) AS total " +
            "FROM transactions t JOIN categories c ON t.category_id = c.id " +
            "WHERE t.user_id = ? AND t.type = 'EXPENSE' " +
            "AND MONTH(t.transaction_date) = ? AND YEAR(t.transaction_date) = ? " +
            "GROUP BY c.name ORDER BY total DESC";

    // -------------------------------------------------------------------------
    // Create / Update / Delete
    // -------------------------------------------------------------------------

    /**
     * Inserts a new transaction and returns the generated id.
     *
     * @param tx the transaction to insert
     * @return the generated transaction id
     * @throws SQLException if a database error occurs
     */
    public int save(Transaction tx) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, tx.getUserId());
            ps.setInt(2, tx.getCategoryId());
            ps.setString(3, tx.getType().name());
            ps.setBigDecimal(4, tx.getAmount());
            ps.setString(5, tx.getDescription());
            ps.setDate(6, Date.valueOf(tx.getTransactionDate()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert failed — no ID obtained.");
    }

    /**
     * Updates an existing transaction.
     *
     * <p>The {@code user_id} in the WHERE clause prevents cross-user updates.</p>
     *
     * @param tx the transaction with updated values (must have id and userId set)
     * @return {@code true} if the row was updated
     * @throws SQLException if a database error occurs
     */
    public boolean update(Transaction tx) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setInt(1, tx.getCategoryId());
            ps.setString(2, tx.getType().name());
            ps.setBigDecimal(3, tx.getAmount());
            ps.setString(4, tx.getDescription());
            ps.setDate(5, Date.valueOf(tx.getTransactionDate()));
            ps.setInt(6, tx.getId());
            ps.setInt(7, tx.getUserId());

            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Deletes a transaction owned by the given user.
     *
     * @param transactionId the id of the transaction to delete
     * @param userId        the id of the owning user
     * @return {@code true} if the row was deleted
     * @throws SQLException if a database error occurs
     */
    public boolean delete(int transactionId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, transactionId);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Finds a single transaction by id, ensuring it belongs to {@code userId}.
     *
     * @param transactionId the transaction id
     * @param userId        the owning user's id
     * @return an {@link Optional} containing the transaction, or empty
     * @throws SQLException if a database error occurs
     */
    public Optional<Transaction> findById(int transactionId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {

            ps.setInt(1, transactionId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all transactions for a user, newest first.
     *
     * @param userId the owning user's id
     * @throws SQLException if a database error occurs
     */
    public List<Transaction> findAllByUser(int userId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL_BY_USER)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns all transactions for a user filtered by category.
     *
     * @param userId     the owning user's id
     * @param categoryId the category id to filter by
     * @throws SQLException if a database error occurs
     */
    public List<Transaction> findByCategory(int userId, int categoryId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_CATEGORY)) {

            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns transactions for a user within a date range (inclusive).
     *
     * @param userId    the owning user's id
     * @param startDate range start (inclusive)
     * @param endDate   range end (inclusive)
     * @throws SQLException if a database error occurs
     */
    public List<Transaction> findByDateRange(int userId, LocalDate startDate,
                                             LocalDate endDate) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_DATE_RANGE)) {

            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Searches transactions by description keyword (case-insensitive LIKE search).
     *
     * @param userId  the owning user's id
     * @param keyword the keyword to search for
     * @throws SQLException if a database error occurs
     */
    public List<Transaction> searchByDescription(int userId, String keyword) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SEARCH_BY_DESCRIPTION)) {

            ps.setInt(1, userId);
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns all transactions for a given user in a specific month/year.
     *
     * @param userId the owning user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @throws SQLException if a database error occurs
     */
    public List<Transaction> findByMonthYear(int userId, int month, int year) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_MONTH_YEAR)) {

            ps.setInt(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns the total amount of a given type (INCOME or EXPENSE) for a
     * specific user and month/year.
     *
     * @param userId the owning user's id
     * @param type   INCOME or EXPENSE
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @throws SQLException if a database error occurs
     */
    public BigDecimal sumByTypeAndMonth(int userId, Transaction.Type type,
                                        int month, int year) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SUM_BY_TYPE_MONTH)) {

            ps.setInt(1, userId);
            ps.setString(2, type.name());
            ps.setInt(3, month);
            ps.setInt(4, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Returns a map (category name → total expenses) for a given month/year.
     *
     * <p>Only EXPENSE transactions are included.</p>
     *
     * @param userId the owning user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @return a linked hash map ordered by descending total
     * @throws SQLException if a database error occurs
     */
    public java.util.Map<String, BigDecimal> sumExpensesByCategory(int userId,
                                                                    int month,
                                                                    int year) throws SQLException {
        java.util.Map<String, BigDecimal> result = new java.util.LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SUM_BY_CATEGORY_MONTH)) {

            ps.setInt(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("name"), rs.getBigDecimal("total"));
                }
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Maps a ResultSet row to a {@link Transaction} object. */
    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setId(rs.getInt("id"));
        tx.setUserId(rs.getInt("user_id"));
        tx.setCategoryId(rs.getInt("category_id"));
        tx.setCategoryName(rs.getString("category_name"));
        tx.setType(Transaction.Type.valueOf(rs.getString("type")));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setDescription(rs.getString("description"));

        Date txDate = rs.getDate("transaction_date");
        if (txDate != null) tx.setTransactionDate(txDate.toLocalDate());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) tx.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) tx.setUpdatedAt(updated.toLocalDateTime());

        return tx;
    }
}
