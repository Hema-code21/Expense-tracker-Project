package com.expensetracker;

import com.expensetracker.model.*;
import com.expensetracker.service.*;
import com.expensetracker.util.CSVExporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Console menu handler — the presentation layer.
 *
 * <p>Reads user input from stdin, delegates to service classes, and prints
 * formatted output to stdout.  Contains no business logic and no SQL.</p>
 *
 * <p>Design note: Each public menu method follows the pattern:
 * <ol>
 *   <li>Print a header.</li>
 *   <li>Prompt for input.</li>
 *   <li>Call the appropriate service method.</li>
 *   <li>Display the result or a meaningful error message.</li>
 * </ol>
 * </p>
 */
public class MenuHandler {

    // ─── ANSI colour codes for a nicer terminal experience ───────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String RED    = "\u001B[31m";
    private static final String BLUE   = "\u001B[34m";

    private final Scanner            scanner;
    private final UserService        userService;
    private final TransactionService transactionService;
    private final ReportService      reportService;
    private final BudgetService      budgetService;

    /** The currently logged-in user; null when no user is logged in. */
    private User currentUser;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public MenuHandler(Scanner scanner) {
        this.scanner            = scanner;
        this.userService        = new UserService();
        ReportService rs        = new ReportService();
        this.reportService      = rs;
        this.transactionService = new TransactionService();
        this.budgetService      = new BudgetService(rs);
    }

    // -------------------------------------------------------------------------
    // Application entry point
    // -------------------------------------------------------------------------

    /**
     * Runs the main application loop.
     *
     * <p>Displays an appropriate menu based on whether a user is logged in
     * and loops until the user chooses to exit.</p>
     */
    public void run() {
        printBanner();
        boolean running = true;
        while (running) {
            if (currentUser == null) {
                running = handleGuestMenu();
            } else {
                running = handleMainMenu();
            }
        }
        println(CYAN + "Thank you for using Expense Tracker. Goodbye!" + RESET);
    }

    // =========================================================================
    // Guest menus (before login)
    // =========================================================================

    private boolean handleGuestMenu() {
        println("\n" + BOLD + "═══ MAIN MENU ═══" + RESET);
        println("  1. Register");
        println("  2. Login");
        println("  0. Exit");
        print(CYAN + "Enter choice: " + RESET);

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> handleRegister();
            case "2" -> handleLogin();
            case "0" -> { return false; }
            default  -> printError("Invalid choice. Please enter 0, 1, or 2.");
        }
        return true;
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    private void handleRegister() {
        printHeader("REGISTER NEW ACCOUNT");

        String username = prompt("Username (3-50 chars, letters/digits/underscores): ");
        String email    = prompt("Email address: ");
        String password = prompt("Password (min 8 chars, must include a letter and digit): ");

        try {
            User user = userService.register(username, email, password);
            printSuccess("Registration successful! Welcome, " + user.getUsername() + "!");
            currentUser = user;
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (RuntimeException e) {
            printError("Registration failed: " + e.getMessage());
        }
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    private void handleLogin() {
        printHeader("LOGIN");

        String username = prompt("Username: ");
        String password = prompt("Password: ");

        try {
            User user = userService.login(username, password);
            currentUser = user;
            printSuccess("Welcome back, " + user.getUsername() + "!");
            checkCurrentMonthBudget(); // Show budget warning if needed
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (RuntimeException e) {
            printError("Login failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // Main menu (after login)
    // =========================================================================

    private boolean handleMainMenu() {
        println("\n" + BOLD + "═══ EXPENSE TRACKER — " + currentUser.getUsername().toUpperCase() + " ═══" + RESET);
        println("  1.  Add Transaction");
        println("  2.  View All Transactions");
        println("  3.  Edit Transaction");
        println("  4.  Delete Transaction");
        println("  5.  Search Transactions");
        println("  6.  Filter by Category");
        println("  7.  Filter by Date Range");
        println("  8.  Monthly Report");
        println("  9.  Set / Update Monthly Budget");
        println("  10. Check Budget Status");
        println("  11. Export Transactions to CSV");
        println("  12. Export Monthly Report to CSV");
        println("  0.  Logout");
        print(CYAN + "Enter choice: " + RESET);

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1"  -> handleAddTransaction();
            case "2"  -> handleViewTransactions();
            case "3"  -> handleEditTransaction();
            case "4"  -> handleDeleteTransaction();
            case "5"  -> handleSearchTransactions();
            case "6"  -> handleFilterByCategory();
            case "7"  -> handleFilterByDateRange();
            case "8"  -> handleMonthlyReport();
            case "9"  -> handleSetBudget();
            case "10" -> handleCheckBudget();
            case "11" -> handleExportTransactions();
            case "12" -> handleExportMonthlyReport();
            case "0"  -> { currentUser = null; println(YELLOW + "Logged out." + RESET); }
            default   -> printError("Invalid choice.");
        }
        return true;
    }

    // =========================================================================
    // Transaction menus
    // =========================================================================

    // ─── Add ──────────────────────────────────────────────────────────────────

    private void handleAddTransaction() {
        printHeader("ADD TRANSACTION");

        List<Category> categories = transactionService.getAllCategories();
        printCategories(categories);

        String catIdStr     = prompt("Category number: ");
        int    categoryId   = parseInt(catIdStr, -1);
        if (categoryId <= 0) { printError("Invalid category number."); return; }

        String typeStr    = promptType();
        String amountStr  = prompt("Amount (e.g., 1500.00): ");
        String desc       = prompt("Description: ");
        String dateStr    = prompt("Date (yyyy-MM-dd) [press Enter for today]: ");
        if (dateStr.isBlank()) dateStr = LocalDate.now().toString();

        try {
            Transaction tx = transactionService.addTransaction(
                    currentUser.getId(), categoryId, typeStr, amountStr, desc, dateStr);
            printSuccess("Transaction added! ID: " + tx.getId());
            checkCurrentMonthBudget();
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (RuntimeException e) {
            printError("Failed to add transaction: " + e.getMessage());
        }
    }

    // ─── View All ─────────────────────────────────────────────────────────────

    private void handleViewTransactions() {
        printHeader("ALL TRANSACTIONS");
        try {
            List<Transaction> list = transactionService.getAllTransactions(currentUser.getId());
            printTransactionTable(list);
        } catch (RuntimeException e) {
            printError(e.getMessage());
        }
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    private void handleEditTransaction() {
        printHeader("EDIT TRANSACTION");

        String idStr = prompt("Enter the ID of the transaction to edit: ");
        int id = parseInt(idStr, -1);
        if (id <= 0) { printError("Invalid transaction ID."); return; }

        try {
            Optional<Transaction> opt = transactionService.getById(id, currentUser.getId());
            if (opt.isEmpty()) { printError("Transaction not found or does not belong to you."); return; }

            Transaction current = opt.get();
            println("\nCurrent values:");
            printTransactionTable(List.of(current));

            List<Category> categories = transactionService.getAllCategories();
            printCategories(categories);

            String catIdStr  = prompt("New category number [current: " + current.getCategoryId() + "]: ");
            int    catId     = catIdStr.isBlank() ? current.getCategoryId() : parseInt(catIdStr, current.getCategoryId());
            String typeStr   = prompt("New type (INCOME/EXPENSE) [current: " + current.getType() + "]: ");
            if (typeStr.isBlank()) typeStr = current.getType().name();
            String amtStr    = prompt("New amount [current: " + current.getAmount() + "]: ");
            if (amtStr.isBlank()) amtStr = current.getAmount().toPlainString();
            String desc      = prompt("New description [current: " + current.getDescription() + "]: ");
            if (desc.isBlank()) desc = current.getDescription();
            String dateStr   = prompt("New date yyyy-MM-dd [current: " + current.getTransactionDate() + "]: ");
            if (dateStr.isBlank()) dateStr = current.getTransactionDate().toString();

            Transaction updated = transactionService.updateTransaction(
                    id, currentUser.getId(), catId, typeStr, amtStr, desc, dateStr);
            printSuccess("Transaction #" + updated.getId() + " updated successfully.");
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (RuntimeException e) {
            printError("Failed to edit transaction: " + e.getMessage());
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    private void handleDeleteTransaction() {
        printHeader("DELETE TRANSACTION");

        String idStr = prompt("Enter the ID of the transaction to delete: ");
        int id = parseInt(idStr, -1);
        if (id <= 0) { printError("Invalid transaction ID."); return; }

        String confirm = prompt("Are you sure you want to delete transaction #" + id + "? (yes/no): ");
        if (!confirm.equalsIgnoreCase("yes")) {
            println(YELLOW + "Deletion cancelled." + RESET);
            return;
        }

        try {
            transactionService.deleteTransaction(id, currentUser.getId());
            printSuccess("Transaction #" + id + " deleted.");
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (RuntimeException e) {
            printError("Failed to delete transaction: " + e.getMessage());
        }
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    private void handleSearchTransactions() {
        printHeader("SEARCH TRANSACTIONS");
        String keyword = prompt("Enter search keyword: ");
        try {
            List<Transaction> list = transactionService.searchByDescription(currentUser.getId(), keyword);
            println("\nFound " + list.size() + " result(s) for \"" + keyword + "\":");
            printTransactionTable(list);
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (RuntimeException e) {
            printError(e.getMessage());
        }
    }

    // ─── Filter by category ───────────────────────────────────────────────────

    private void handleFilterByCategory() {
        printHeader("FILTER BY CATEGORY");

        List<Category> categories = transactionService.getAllCategories();
        printCategories(categories);

        String catIdStr = prompt("Category number: ");
        int catId = parseInt(catIdStr, -1);
        if (catId <= 0) { printError("Invalid category number."); return; }

        try {
            List<Transaction> list = transactionService.getByCategory(currentUser.getId(), catId);
            println("\nTransactions in selected category:");
            printTransactionTable(list);
        } catch (RuntimeException e) {
            printError(e.getMessage());
        }
    }

    // ─── Filter by date range ─────────────────────────────────────────────────

    private void handleFilterByDateRange() {
        printHeader("FILTER BY DATE RANGE");
        String startDate = prompt("Start date (yyyy-MM-dd): ");
        String endDate   = prompt("End date   (yyyy-MM-dd): ");
        try {
            List<Transaction> list = transactionService.getByDateRange(
                    currentUser.getId(), startDate, endDate);
            println("\nTransactions from " + startDate + " to " + endDate + ":");
            printTransactionTable(list);
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (RuntimeException e) {
            printError(e.getMessage());
        }
    }

    // =========================================================================
    // Report menus
    // =========================================================================

    private void handleMonthlyReport() {
        printHeader("MONTHLY REPORT");
        int[] my = promptMonthYear();
        int month = my[0], year = my[1];

        try {
            ReportService.MonthlyReport report = reportService.getMonthlyReport(
                    currentUser.getId(), month, year);
            printMonthlyReport(report);

            // Budget status alongside the report
            BudgetService.BudgetStatus status =
                    budgetService.checkBudgetStatus(currentUser.getId(), month, year);
            printBudgetStatus(status, month, year);

        } catch (RuntimeException e) {
            printError(e.getMessage());
        }
    }

    // =========================================================================
    // Budget menus
    // =========================================================================

    private void handleSetBudget() {
        printHeader("SET / UPDATE MONTHLY BUDGET");
        int[] my = promptMonthYear();
        int month = my[0], year = my[1];
        String amountStr = prompt("Budget amount: ");

        try {
            Budget budget = budgetService.setBudget(currentUser.getId(), month, year, amountStr);
            printSuccess(String.format("Budget for %s %d set to ₹%s",
                    monthName(month), year, budget.getAmount().toPlainString()));
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (RuntimeException e) {
            printError("Failed to set budget: " + e.getMessage());
        }
    }

    private void handleCheckBudget() {
        printHeader("BUDGET STATUS");
        int[] my = promptMonthYear();
        int month = my[0], year = my[1];

        BudgetService.BudgetStatus status =
                budgetService.checkBudgetStatus(currentUser.getId(), month, year);
        printBudgetStatus(status, month, year);
    }

    // =========================================================================
    // CSV export menus
    // =========================================================================

    private void handleExportTransactions() {
        printHeader("EXPORT TRANSACTIONS TO CSV");
        try {
            List<Transaction> list = transactionService.getAllTransactions(currentUser.getId());
            if (list.isEmpty()) { println(YELLOW + "No transactions to export." + RESET); return; }

            String path = CSVExporter.exportTransactions(list, currentUser.getUsername());
            printSuccess("Exported " + list.size() + " transactions to:\n  " + path);
        } catch (IOException e) {
            printError("Failed to write CSV: " + e.getMessage());
        } catch (RuntimeException e) {
            printError("Export failed: " + e.getMessage());
        }
    }

    private void handleExportMonthlyReport() {
        printHeader("EXPORT MONTHLY REPORT TO CSV");
        int[] my = promptMonthYear();
        int month = my[0], year = my[1];

        try {
            ReportService.MonthlyReport report = reportService.getMonthlyReport(
                    currentUser.getId(), month, year);

            String path = CSVExporter.exportMonthlyReport(
                    month, year,
                    report.getTotalIncome(),
                    report.getTotalExpenses(),
                    report.getBalance(),
                    report.getCategoryBreakdown(),
                    currentUser.getUsername());

            printSuccess("Monthly report exported to:\n  " + path);
        } catch (IOException e) {
            printError("Failed to write CSV: " + e.getMessage());
        } catch (RuntimeException e) {
            printError("Export failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // Helper: budget status check on login / after adding a transaction
    // =========================================================================

    private void checkCurrentMonthBudget() {
        LocalDate now = LocalDate.now();
        try {
            BudgetService.BudgetStatus status =
                    budgetService.checkBudgetStatus(currentUser.getId(), now.getMonthValue(), now.getYear());
            if (status.hasBudget()) {
                if (status.isExceeded()) {
                    println(RED + BOLD +
                            "\n⚠  WARNING: You have exceeded your budget for " +
                            monthName(now.getMonthValue()) + " " + now.getYear() + "!" +
                            "\n   Budget: ₹" + status.getBudgetAmount() +
                            "  |  Spent: ₹" + status.getTotalExpenses() +
                            "  |  Over by: ₹" + status.getTotalExpenses().subtract(status.getBudgetAmount()).abs() +
                            RESET);
                } else if (status.isNearLimit()) {
                    println(YELLOW + BOLD +
                            "\n⚡ ALERT: You have used 90%+ of your " +
                            monthName(now.getMonthValue()) + " " + now.getYear() + " budget." +
                            "\n   Budget: ₹" + status.getBudgetAmount() +
                            "  |  Spent: ₹" + status.getTotalExpenses() +
                            "  |  Remaining: ₹" + status.getRemaining() +
                            RESET);
                }
            }
        } catch (RuntimeException ignored) {
            // Budget check is non-critical; swallow silently
        }
    }

    // =========================================================================
    // Formatting helpers
    // =========================================================================

    private void printBanner() {
        println(CYAN + BOLD);
        println("╔══════════════════════════════════════════╗");
        println("║         💰  EXPENSE TRACKER  💰          ║");
        println("║   Java 17 · JDBC · MySQL 8 · v1.0.0     ║");
        println("╚══════════════════════════════════════════╝");
        println(RESET);
    }

    private void printHeader(String title) {
        println("\n" + BOLD + BLUE + "─── " + title + " ───" + RESET);
    }

    private void printSuccess(String msg) {
        println(GREEN + "✔  " + msg + RESET);
    }

    private void printError(String msg) {
        println(RED + "✖  " + msg + RESET);
    }

    private void printCategories(List<Category> categories) {
        println("\nAvailable categories:");
        for (Category c : categories) {
            System.out.printf("  %2d. %s%n", c.getId(), c.getName());
        }
    }

    private void printTransactionTable(List<Transaction> list) {
        if (list.isEmpty()) {
            println(YELLOW + "  No transactions found." + RESET);
            return;
        }
        println(BOLD +
                String.format("  %-6s %-9s %-12s %-12s %-12s %s",
                        "ID", "TYPE", "CATEGORY", "AMOUNT", "DATE", "DESCRIPTION") +
                RESET);
        println("  " + "─".repeat(80));
        for (Transaction t : list) {
            String colour = t.getType() == Transaction.Type.INCOME ? GREEN : RED;
            System.out.printf("  %-6d %s%-9s%s %-12s %-12s %-12s %s%n",
                    t.getId(),
                    colour, t.getType(), RESET,
                    t.getCategoryName() != null ? t.getCategoryName() : "-",
                    "₹" + t.getAmount().toPlainString(),
                    t.getTransactionDate() != null ? t.getTransactionDate().toString() : "-",
                    t.getDescription());
        }
        println("  Total: " + list.size() + " transaction(s)");
    }

    private void printMonthlyReport(ReportService.MonthlyReport report) {
        println("\n" + BOLD + CYAN +
                String.format("  Monthly Report — %s %d", monthName(report.getMonth()), report.getYear()) +
                RESET);
        println("  " + "─".repeat(50));
        println(GREEN  + String.format("  Total Income   : ₹%-15s", report.getTotalIncome())  + RESET);
        println(RED    + String.format("  Total Expenses : ₹%-15s", report.getTotalExpenses()) + RESET);

        String balanceColour = report.getBalance().compareTo(BigDecimal.ZERO) >= 0 ? GREEN : RED;
        println(balanceColour + String.format("  Net Balance    : ₹%-15s", report.getBalance()) + RESET);

        if (!report.getCategoryBreakdown().isEmpty()) {
            println("\n  " + BOLD + "Category-wise Expenses:" + RESET);
            println("  " + "─".repeat(40));
            report.getCategoryBreakdown().forEach((cat, amt) ->
                    System.out.printf("  %-15s : ₹%s%n", cat, amt.toPlainString()));
        }

        println("\n  Transactions this month: " + report.getTransactions().size());
    }

    private void printBudgetStatus(BudgetService.BudgetStatus status, int month, int year) {
        println("\n" + BOLD + "  Budget Status — " + monthName(month) + " " + year + RESET);
        println("  " + "─".repeat(40));
        if (!status.hasBudget()) {
            println(YELLOW + "  No budget set for this period." + RESET);
            return;
        }
        println("  Budget   : ₹" + status.getBudgetAmount());
        println("  Spent    : ₹" + status.getTotalExpenses());
        String remColour = status.getRemaining().compareTo(BigDecimal.ZERO) >= 0 ? GREEN : RED;
        println(remColour + "  Remaining: ₹" + status.getRemaining() + RESET);

        if (status.isExceeded()) {
            println(RED + BOLD + "\n  ⚠  Budget EXCEEDED!" + RESET);
        } else if (status.isNearLimit()) {
            println(YELLOW + BOLD + "\n  ⚡ Approaching budget limit (≥90% used)" + RESET);
        } else {
            println(GREEN + "\n  ✔  Within budget" + RESET);
        }
    }

    // =========================================================================
    // Input helpers
    // =========================================================================

    private String prompt(String message) {
        print(CYAN + message + RESET);
        return scanner.nextLine().trim();
    }

    private String promptType() {
        println("Transaction type:");
        println("  1. INCOME");
        println("  2. EXPENSE");
        String choice = prompt("Enter 1 or 2: ");
        return switch (choice) {
            case "1" -> "INCOME";
            case "2" -> "EXPENSE";
            default  -> choice.toUpperCase(); // let the service validate
        };
    }

    /**
     * Prompts for month and year, defaulting to the current month/year if blank.
     *
     * @return int[2] where [0] = month and [1] = year
     */
    private int[] promptMonthYear() {
        LocalDate now = LocalDate.now();
        String monthStr = prompt("Month (1-12) [Enter for current: " + now.getMonthValue() + "]: ");
        String yearStr  = prompt("Year (e.g., 2024) [Enter for current: " + now.getYear() + "]: ");

        int month = monthStr.isBlank() ? now.getMonthValue() : parseInt(monthStr, now.getMonthValue());
        int year  = yearStr.isBlank()  ? now.getYear()       : parseInt(yearStr,  now.getYear());

        return new int[]{month, year};
    }

    private int parseInt(String s, int defaultValue) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private void print(String s)   { System.out.print(s); }
    private void println(String s) { System.out.println(s); }
}
