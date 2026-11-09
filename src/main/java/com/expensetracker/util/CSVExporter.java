package com.expensetracker.util;

import com.expensetracker.model.Transaction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exports data to CSV files in the {@code exports/} directory.
 *
 * <p>All files are written with UTF-8 encoding. The exports directory is
 * created automatically if it does not exist.</p>
 */
public final class CSVExporter {

    private static final String EXPORTS_DIR = "exports";
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Prevent instantiation
    private CSVExporter() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Exports a list of transactions to a CSV file.
     *
     * @param transactions the transactions to export
     * @param username     owner's username (used in the file name)
     * @return the absolute path of the created file
     * @throws IOException if writing fails
     */
    public static String exportTransactions(List<Transaction> transactions,
                                            String username) throws IOException {
        ensureExportsDir();
        String fileName = String.format("%s_transactions_%s.csv",
                sanitise(username), LocalDateTime.now().format(FILE_TS));
        Path filePath = Paths.get(EXPORTS_DIR, fileName);

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath.toFile(), java.nio.charset.StandardCharsets.UTF_8))) {
            // Header
            pw.println("ID,Type,Category,Amount,Description,Date");

            // Rows
            for (Transaction t : transactions) {
                pw.printf("%d,%s,%s,%s,%s,%s%n",
                        t.getId(),
                        t.getType(),
                        escapeCsv(t.getCategoryName()),
                        t.getAmount().toPlainString(),
                        escapeCsv(t.getDescription()),
                        t.getTransactionDate() != null
                                ? t.getTransactionDate().format(DATE_FMT) : "");
            }
        }

        return filePath.toAbsolutePath().toString();
    }

    /**
     * Exports a monthly report summary to a CSV file.
     *
     * @param month              calendar month (1-12)
     * @param year               calendar year
     * @param totalIncome        total income for the period
     * @param totalExpenses      total expenses for the period
     * @param balance            net balance
     * @param categoryBreakdown  map of category name → amount
     * @param username           owner's username
     * @return the absolute path of the created file
     * @throws IOException if writing fails
     */
    public static String exportMonthlyReport(int month, int year,
                                             BigDecimal totalIncome,
                                             BigDecimal totalExpenses,
                                             BigDecimal balance,
                                             Map<String, BigDecimal> categoryBreakdown,
                                             String username) throws IOException {
        ensureExportsDir();
        String fileName = String.format("%s_report_%04d_%02d_%s.csv",
                sanitise(username), year, month, LocalDateTime.now().format(FILE_TS));
        Path filePath = Paths.get(EXPORTS_DIR, fileName);

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath.toFile(), java.nio.charset.StandardCharsets.UTF_8))) {
            // Summary section
            pw.println("Report Period,Total Income,Total Expenses,Balance");
            pw.printf("%s-%02d,%s,%s,%s%n",
                    year, month,
                    totalIncome.toPlainString(),
                    totalExpenses.toPlainString(),
                    balance.toPlainString());

            pw.println(); // blank line

            // Category breakdown section
            pw.println("Category,Expenses");
            for (Map.Entry<String, BigDecimal> entry : categoryBreakdown.entrySet()) {
                pw.printf("%s,%s%n",
                        escapeCsv(entry.getKey()),
                        entry.getValue().toPlainString());
            }
        }

        return filePath.toAbsolutePath().toString();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Creates the exports directory if it does not already exist. */
    private static void ensureExportsDir() throws IOException {
        Path dir = Paths.get(EXPORTS_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    /**
     * Escapes a CSV field by wrapping in double-quotes if it contains a
     * comma, double-quote, or newline character.
     */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Removes characters unsafe for file names. */
    private static String sanitise(String name) {
        return name == null ? "user" : name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
