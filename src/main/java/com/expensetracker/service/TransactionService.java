package com.expensetracker.service;

import com.expensetracker.dao.CategoryDAO;
import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.model.Category;
import com.expensetracker.model.Transaction;
import com.expensetracker.util.InputValidator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for managing transactions.
 *
 * <p>Enforces input validation and user-ownership rules before delegating
 * to the DAO layer.  The calling layer should always supply the
 * authenticated user's id.</p>
 */
public class TransactionService {

    private final TransactionDAO transactionDAO;
    private final CategoryDAO    categoryDAO;

    public TransactionService() {
        this.transactionDAO = new TransactionDAO();
        this.categoryDAO    = new CategoryDAO();
    }

    /** Constructor for dependency injection (useful in tests). */
    public TransactionService(TransactionDAO transactionDAO, CategoryDAO categoryDAO) {
        this.transactionDAO = transactionDAO;
        this.categoryDAO    = categoryDAO;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Adds a new transaction for the authenticated user.
     *
     * @param userId      the id of the authenticated user
     * @param categoryId  the id of the chosen category
     * @param typeStr     "INCOME" or "EXPENSE" (case-insensitive)
     * @param amountStr   monetary amount as a string (e.g., "1500.00")
     * @param description human-readable description
     * @param dateStr     transaction date in {@code yyyy-MM-dd} format
     * @return the saved {@link Transaction} with its generated id
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         wrapping any {@link SQLException}
     */
    public Transaction addTransaction(int userId, int categoryId, String typeStr,
                                      String amountStr, String description, String dateStr) {
        // Validate
        List<String> errors = new java.util.ArrayList<>();
        errors.addAll(InputValidator.validateAmount(amountStr));
        errors.addAll(InputValidator.validateDescription(description));
        errors.addAll(InputValidator.validateDate(dateStr));

        Transaction.Type type = parseType(typeStr, errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        try {
            // Verify category exists
            Optional<Category> cat = categoryDAO.findById(categoryId);
            if (cat.isEmpty()) {
                throw new IllegalArgumentException("Invalid category id: " + categoryId);
            }

            Transaction tx = new Transaction(
                    userId, categoryId, type,
                    new BigDecimal(amountStr.trim()),
                    description.trim(),
                    LocalDate.parse(dateStr.trim()));

            int id = transactionDAO.save(tx);
            tx.setId(id);
            tx.setCategoryName(cat.get().getName());
            return tx;

        } catch (SQLException e) {
            throw new RuntimeException("Database error adding transaction: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns all transactions for the authenticated user.
     *
     * @param userId the authenticated user's id
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public List<Transaction> getAllTransactions(int userId) {
        try {
            return transactionDAO.findAllByUser(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a single transaction by id, verifying ownership.
     *
     * @param transactionId the transaction id
     * @param userId        the authenticated user's id
     * @return an {@link Optional} containing the transaction, or empty
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public Optional<Transaction> getById(int transactionId, int userId) {
        try {
            return transactionDAO.findById(transactionId, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Returns transactions filtered by category.
     *
     * @param userId     the authenticated user's id
     * @param categoryId the category id to filter by
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public List<Transaction> getByCategory(int userId, int categoryId) {
        try {
            return transactionDAO.findByCategory(userId, categoryId);
        } catch (SQLException e) {
            throw new RuntimeException("Database error filtering by category: " + e.getMessage(), e);
        }
    }

    /**
     * Returns transactions within a date range.
     *
     * @param userId       the authenticated user's id
     * @param startDateStr start date in {@code yyyy-MM-dd} format
     * @param endDateStr   end date in {@code yyyy-MM-dd} format
     * @throws IllegalArgumentException if dates are invalid or end is before start
     * @throws RuntimeException         wrapping any {@link SQLException}
     */
    public List<Transaction> getByDateRange(int userId, String startDateStr, String endDateStr) {
        List<String> errors = new java.util.ArrayList<>();
        errors.addAll(InputValidator.validateDate(startDateStr));
        errors.addAll(InputValidator.validateDate(endDateStr));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        LocalDate start = LocalDate.parse(startDateStr.trim());
        LocalDate end   = LocalDate.parse(endDateStr.trim());
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be on or after start date.");
        }

        try {
            return transactionDAO.findByDateRange(userId, start, end);
        } catch (SQLException e) {
            throw new RuntimeException("Database error filtering by date range: " + e.getMessage(), e);
        }
    }

    /**
     * Searches transactions by a description keyword.
     *
     * @param userId  the authenticated user's id
     * @param keyword the search keyword
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public List<Transaction> searchByDescription(int userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be blank.");
        }
        try {
            return transactionDAO.searchByDescription(userId, keyword.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Database error searching transactions: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    /**
     * Edits an existing transaction.
     *
     * @param transactionId  the id of the transaction to edit
     * @param userId         the authenticated user's id
     * @param categoryId     new category id
     * @param typeStr        "INCOME" or "EXPENSE"
     * @param amountStr      new amount as a string
     * @param description    new description
     * @param dateStr        new date in {@code yyyy-MM-dd} format
     * @return the updated {@link Transaction}
     * @throws IllegalArgumentException if validation fails or transaction not found
     * @throws RuntimeException         wrapping any {@link SQLException}
     */
    public Transaction updateTransaction(int transactionId, int userId,
                                         int categoryId, String typeStr,
                                         String amountStr, String description,
                                         String dateStr) {
        // Validate input
        List<String> errors = new java.util.ArrayList<>();
        errors.addAll(InputValidator.validateAmount(amountStr));
        errors.addAll(InputValidator.validateDescription(description));
        errors.addAll(InputValidator.validateDate(dateStr));

        Transaction.Type type = parseType(typeStr, errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        try {
            // Verify ownership
            Optional<Transaction> existing = transactionDAO.findById(transactionId, userId);
            if (existing.isEmpty()) {
                throw new IllegalArgumentException("Transaction not found or does not belong to you.");
            }

            // Verify category exists
            Optional<Category> cat = categoryDAO.findById(categoryId);
            if (cat.isEmpty()) {
                throw new IllegalArgumentException("Invalid category id: " + categoryId);
            }

            Transaction tx = existing.get();
            tx.setCategoryId(categoryId);
            tx.setType(type);
            tx.setAmount(new BigDecimal(amountStr.trim()));
            tx.setDescription(description.trim());
            tx.setTransactionDate(LocalDate.parse(dateStr.trim()));

            transactionDAO.update(tx);
            tx.setCategoryName(cat.get().getName());
            return tx;

        } catch (SQLException e) {
            throw new RuntimeException("Database error updating transaction: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /**
     * Deletes a transaction owned by the authenticated user.
     *
     * @param transactionId the id of the transaction to delete
     * @param userId        the authenticated user's id
     * @return {@code true} if the transaction was deleted
     * @throws IllegalArgumentException if the transaction does not belong to the user
     * @throws RuntimeException         wrapping any {@link SQLException}
     */
    public boolean deleteTransaction(int transactionId, int userId) {
        try {
            boolean deleted = transactionDAO.delete(transactionId, userId);
            if (!deleted) {
                throw new IllegalArgumentException(
                        "Transaction not found or does not belong to you.");
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Database error deleting transaction: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Category helper
    // -------------------------------------------------------------------------

    /**
     * Returns all available categories.
     *
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public List<Category> getAllCategories() {
        try {
            return categoryDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching categories: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Transaction.Type parseType(String typeStr, List<String> errors) {
        if (typeStr == null || typeStr.isBlank()) {
            errors.add("Transaction type must not be blank.");
            return null;
        }
        try {
            return Transaction.Type.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add("Transaction type must be INCOME or EXPENSE.");
            return null;
        }
    }
}
