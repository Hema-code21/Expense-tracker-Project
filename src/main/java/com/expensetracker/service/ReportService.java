package com.expensetracker.service;

import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.model.Transaction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Generates financial reports for a user.
 *
 * <p>All report methods are read-only and accept the authenticated user's id
 * to guarantee data isolation.</p>
 */
public class ReportService {

    private final TransactionDAO transactionDAO;

    public ReportService() {
        this.transactionDAO = new TransactionDAO();
    }

    /** Constructor for dependency injection (useful in tests). */
    public ReportService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    // -------------------------------------------------------------------------
    // Monthly summary figures
    // -------------------------------------------------------------------------

    /**
     * Returns the total income for a given user and month/year.
     *
     * @param userId the authenticated user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public BigDecimal getTotalIncome(int userId, int month, int year) {
        try {
            return transactionDAO.sumByTypeAndMonth(userId, Transaction.Type.INCOME, month, year);
        } catch (SQLException e) {
            throw new RuntimeException("Database error computing income: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the total expenses for a given user and month/year.
     *
     * @param userId the authenticated user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public BigDecimal getTotalExpenses(int userId, int month, int year) {
        try {
            return transactionDAO.sumByTypeAndMonth(userId, Transaction.Type.EXPENSE, month, year);
        } catch (SQLException e) {
            throw new RuntimeException("Database error computing expenses: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the net balance (income − expenses) for a given user and month/year.
     *
     * @param userId the authenticated user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     */
    public BigDecimal getBalance(int userId, int month, int year) {
        BigDecimal income   = getTotalIncome(userId, month, year);
        BigDecimal expenses = getTotalExpenses(userId, month, year);
        return income.subtract(expenses);
    }

    // -------------------------------------------------------------------------
    // Category breakdown
    // -------------------------------------------------------------------------

    /**
     * Returns a map of category name → total expenses, ordered by descending
     * amount, for a given user and month/year.
     *
     * @param userId the authenticated user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public Map<String, BigDecimal> getExpensesByCategory(int userId, int month, int year) {
        try {
            return transactionDAO.sumExpensesByCategory(userId, month, year);
        } catch (SQLException e) {
            throw new RuntimeException("Database error computing category breakdown: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Transaction lists (for CSV export)
    // -------------------------------------------------------------------------

    /**
     * Returns all transactions for a given user and month/year (used by CSV export).
     *
     * @param userId the authenticated user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public List<Transaction> getTransactionsForMonth(int userId, int month, int year) {
        try {
            return transactionDAO.findByMonthYear(userId, month, year);
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching monthly transactions: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Full monthly report (value object)
    // -------------------------------------------------------------------------

    /**
     * Builds a complete monthly report for display or export.
     *
     * @param userId the authenticated user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @return a {@link MonthlyReport} value object
     */
    public MonthlyReport getMonthlyReport(int userId, int month, int year) {
        BigDecimal income        = getTotalIncome(userId, month, year);
        BigDecimal expenses      = getTotalExpenses(userId, month, year);
        BigDecimal balance       = income.subtract(expenses);
        Map<String, BigDecimal> categoryBreakdown = getExpensesByCategory(userId, month, year);
        List<Transaction> transactions            = getTransactionsForMonth(userId, month, year);

        return new MonthlyReport(month, year, income, expenses, balance,
                categoryBreakdown, transactions);
    }

    // -------------------------------------------------------------------------
    // Inner class: MonthlyReport
    // -------------------------------------------------------------------------

    /**
     * Immutable value object containing all data for a monthly financial report.
     */
    public static final class MonthlyReport {

        private final int    month;
        private final int    year;
        private final BigDecimal totalIncome;
        private final BigDecimal totalExpenses;
        private final BigDecimal balance;
        private final Map<String, BigDecimal> categoryBreakdown;
        private final List<Transaction>       transactions;

        public MonthlyReport(int month, int year,
                             BigDecimal totalIncome, BigDecimal totalExpenses,
                             BigDecimal balance,
                             Map<String, BigDecimal> categoryBreakdown,
                             List<Transaction> transactions) {
            this.month             = month;
            this.year              = year;
            this.totalIncome       = totalIncome;
            this.totalExpenses     = totalExpenses;
            this.balance           = balance;
            this.categoryBreakdown = categoryBreakdown;
            this.transactions      = transactions;
        }

        public int getMonth()                                { return month; }
        public int getYear()                                 { return year; }
        public BigDecimal getTotalIncome()                   { return totalIncome; }
        public BigDecimal getTotalExpenses()                 { return totalExpenses; }
        public BigDecimal getBalance()                       { return balance; }
        public Map<String, BigDecimal> getCategoryBreakdown(){ return categoryBreakdown; }
        public List<Transaction> getTransactions()           { return transactions; }
    }
}
