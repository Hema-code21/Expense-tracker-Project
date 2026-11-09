package com.expensetracker.service;

import com.expensetracker.dao.BudgetDAO;
import com.expensetracker.model.Budget;
import com.expensetracker.util.InputValidator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for monthly budget management.
 *
 * <p>A user may set or update one budget per calendar month.  When the
 * current month's expenses exceed the budget, the service returns a
 * warning flag so the UI can alert the user.</p>
 */
public class BudgetService {

    private final BudgetDAO        budgetDAO;
    private final ReportService    reportService;

    public BudgetService(ReportService reportService) {
        this.budgetDAO     = new BudgetDAO();
        this.reportService = reportService;
    }

    /** Constructor for dependency injection (useful in tests). */
    public BudgetService(BudgetDAO budgetDAO, ReportService reportService) {
        this.budgetDAO     = budgetDAO;
        this.reportService = reportService;
    }

    // -------------------------------------------------------------------------
    // Set / Update budget
    // -------------------------------------------------------------------------

    /**
     * Sets or updates the monthly budget for the authenticated user.
     *
     * @param userId    the authenticated user's id
     * @param month     calendar month (1-12)
     * @param year      calendar year
     * @param amountStr budget amount as a string
     * @return the saved {@link Budget}
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         wrapping any {@link SQLException}
     */
    public Budget setBudget(int userId, int month, int year, String amountStr) {
        List<String> errors = new java.util.ArrayList<>();
        errors.addAll(InputValidator.validateMonth(month));
        errors.addAll(InputValidator.validateYear(year));
        errors.addAll(InputValidator.validateAmount(amountStr));

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        Budget budget = new Budget(userId, month, year, new BigDecimal(amountStr.trim()));
        try {
            budgetDAO.saveOrUpdate(budget);
            // Fetch back to get id / timestamps
            Optional<Budget> saved = budgetDAO.findByUserMonthYear(userId, month, year);
            return saved.orElse(budget);
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving budget: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Read budget
    // -------------------------------------------------------------------------

    /**
     * Returns the budget for a specific user and month/year, if one exists.
     *
     * @param userId the authenticated user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @return an {@link Optional} containing the budget
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public Optional<Budget> getBudget(int userId, int month, int year) {
        try {
            return budgetDAO.findByUserMonthYear(userId, month, year);
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching budget: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Budget status check
    // -------------------------------------------------------------------------

    /**
     * Checks whether the user's expenses for a given month exceed their budget.
     *
     * @param userId the authenticated user's id
     * @param month  calendar month (1-12)
     * @param year   calendar year
     * @return a {@link BudgetStatus} summarising the situation
     */
    public BudgetStatus checkBudgetStatus(int userId, int month, int year) {
        Optional<Budget> budgetOpt = getBudget(userId, month, year);
        BigDecimal totalExpenses = reportService.getTotalExpenses(userId, month, year);

        if (budgetOpt.isEmpty()) {
            return new BudgetStatus(null, totalExpenses, false, false);
        }

        BigDecimal budgetAmount = budgetOpt.get().getAmount();
        boolean exceeded = totalExpenses.compareTo(budgetAmount) > 0;
        boolean nearLimit = !exceeded &&
                totalExpenses.compareTo(budgetAmount.multiply(new BigDecimal("0.90"))) >= 0;

        return new BudgetStatus(budgetAmount, totalExpenses, exceeded, nearLimit);
    }

    // -------------------------------------------------------------------------
    // Inner class: BudgetStatus
    // -------------------------------------------------------------------------

    /**
     * Immutable value object returned by {@link #checkBudgetStatus}.
     */
    public static final class BudgetStatus {

        private final BigDecimal budgetAmount;   // null if no budget set
        private final BigDecimal totalExpenses;
        private final boolean    exceeded;
        private final boolean    nearLimit;      // ≥ 90% of budget

        public BudgetStatus(BigDecimal budgetAmount, BigDecimal totalExpenses,
                            boolean exceeded, boolean nearLimit) {
            this.budgetAmount  = budgetAmount;
            this.totalExpenses = totalExpenses;
            this.exceeded      = exceeded;
            this.nearLimit     = nearLimit;
        }

        public boolean hasBudget()             { return budgetAmount != null; }
        public BigDecimal getBudgetAmount()    { return budgetAmount; }
        public BigDecimal getTotalExpenses()   { return totalExpenses; }
        public boolean isExceeded()            { return exceeded; }
        public boolean isNearLimit()           { return nearLimit; }

        /** Remaining budget (negative if exceeded). */
        public BigDecimal getRemaining() {
            if (budgetAmount == null) return BigDecimal.ZERO;
            return budgetAmount.subtract(totalExpenses);
        }
    }
}
