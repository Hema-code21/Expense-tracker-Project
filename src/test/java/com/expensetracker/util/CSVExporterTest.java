package com.expensetracker.util;

import com.expensetracker.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CSVExporter}.
 *
 * <p>Uses JUnit 5's {@code @TempDir} to write files into a temporary directory
 * that is automatically cleaned up after each test. Because CSVExporter
 * hard-codes the {@code exports/} directory relative to the working directory,
 * these tests verify the file content after reading it back.</p>
 */
@DisplayName("CSVExporter Tests")
class CSVExporterTest {

    // We rely on the real exports/ directory created by CSVExporter.
    // Clean it up after each test.
    private Path exportedFile;

    @AfterEach
    void cleanup() throws IOException {
        if (exportedFile != null && Files.exists(exportedFile)) {
            Files.delete(exportedFile);
        }
    }

    // ─── exportTransactions ───────────────────────────────────────────────────

    @Test
    @DisplayName("exportTransactions creates a file with a header and data rows")
    void testExportTransactions_fileCreated() throws IOException {
        List<Transaction> transactions = buildSampleTransactions();

        String path = CSVExporter.exportTransactions(transactions, "testuser");
        exportedFile = Path.of(path);

        assertTrue(Files.exists(exportedFile), "Exported file should exist");

        List<String> lines = Files.readAllLines(exportedFile);
        // Header + 2 data rows
        assertEquals(3, lines.size(), "File should have 1 header + 2 data rows");
        assertTrue(lines.get(0).startsWith("ID,Type,Category"), "First line should be the header");
    }

    @Test
    @DisplayName("exportTransactions correctly formats INCOME and EXPENSE rows")
    void testExportTransactions_contentCorrect() throws IOException {
        List<Transaction> transactions = buildSampleTransactions();

        String path = CSVExporter.exportTransactions(transactions, "alice");
        exportedFile = Path.of(path);

        String content = Files.readString(exportedFile);
        assertTrue(content.contains("INCOME"),  "File should contain INCOME");
        assertTrue(content.contains("EXPENSE"), "File should contain EXPENSE");
        assertTrue(content.contains("Salary"),  "File should contain Salary category");
        assertTrue(content.contains("5000.00"), "File should contain the income amount");
    }

    @Test
    @DisplayName("exportTransactions handles commas in description with CSV quoting")
    void testExportTransactions_csvEscaping() throws IOException {
        Transaction tx = new Transaction();
        tx.setId(99);
        tx.setType(Transaction.Type.EXPENSE);
        tx.setCategoryName("Food");
        tx.setAmount(new BigDecimal("250.00"));
        tx.setDescription("Lunch, snacks, and drinks"); // contains commas
        tx.setTransactionDate(LocalDate.of(2024, 3, 15));

        String path = CSVExporter.exportTransactions(List.of(tx), "bob");
        exportedFile = Path.of(path);

        String content = Files.readString(exportedFile);
        // The description with commas should be wrapped in double-quotes
        assertTrue(content.contains("\"Lunch, snacks, and drinks\""),
                "Description with commas should be CSV-quoted");
    }

    // ─── exportMonthlyReport ──────────────────────────────────────────────────

    @Test
    @DisplayName("exportMonthlyReport creates a file with summary and category sections")
    void testExportMonthlyReport_fileCreated() throws IOException {
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put("Food",   new BigDecimal("1200.00"));
        breakdown.put("Travel", new BigDecimal("800.00"));

        String path = CSVExporter.exportMonthlyReport(
                3, 2024,
                new BigDecimal("5000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("3000.00"),
                breakdown,
                "alice");
        exportedFile = Path.of(path);

        assertTrue(Files.exists(exportedFile), "Report file should exist");

        List<String> lines = Files.readAllLines(exportedFile);
        // Header + summary row + blank line + category header + 2 category rows = 6 lines
        assertTrue(lines.size() >= 6, "Report should have at least 6 lines");
    }

    @Test
    @DisplayName("exportMonthlyReport file name contains the year and month")
    void testExportMonthlyReport_fileName() throws IOException {
        String path = CSVExporter.exportMonthlyReport(
                7, 2024,
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(9),
                new LinkedHashMap<>(),
                "charlie");
        exportedFile = Path.of(path);

        assertTrue(path.contains("2024"), "File name should contain the year");
        assertTrue(path.contains("07"),   "File name should contain zero-padded month");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<Transaction> buildSampleTransactions() {
        Transaction income = new Transaction();
        income.setId(1);
        income.setType(Transaction.Type.INCOME);
        income.setCategoryName("Salary");
        income.setAmount(new BigDecimal("5000.00"));
        income.setDescription("Monthly salary");
        income.setTransactionDate(LocalDate.of(2024, 3, 1));

        Transaction expense = new Transaction();
        expense.setId(2);
        expense.setType(Transaction.Type.EXPENSE);
        expense.setCategoryName("Food");
        expense.setAmount(new BigDecimal("450.50"));
        expense.setDescription("Groceries");
        expense.setTransactionDate(LocalDate.of(2024, 3, 5));

        return List.of(income, expense);
    }
}
