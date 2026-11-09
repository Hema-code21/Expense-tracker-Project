package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single financial transaction (income or expense).
 *
 * <p>Each transaction belongs to a user and a category.</p>
 */
public class Transaction {

    /** Discriminates between money coming in and money going out. */
    public enum Type {
        INCOME, EXPENSE
    }

    private int id;
    private int userId;
    private int categoryId;
    private String categoryName;    // Denormalised for display convenience
    private Type type;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Transaction() {}

    /** Constructor for creating a new transaction (no id yet). */
    public Transaction(int userId, int categoryId, Type type,
                       BigDecimal amount, String description,
                       LocalDate transactionDate) {
        this.userId          = userId;
        this.categoryId      = categoryId;
        this.type            = type;
        this.amount          = amount;
        this.description     = description;
        this.transactionDate = transactionDate;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }

    public int getUserId()                       { return userId; }
    public void setUserId(int userId)            { this.userId = userId; }

    public int getCategoryId()                   { return categoryId; }
    public void setCategoryId(int categoryId)    { this.categoryId = categoryId; }

    public String getCategoryName()              { return categoryName; }
    public void setCategoryName(String n)        { this.categoryName = n; }

    public Type getType()                        { return type; }
    public void setType(Type type)               { this.type = type; }

    public BigDecimal getAmount()                { return amount; }
    public void setAmount(BigDecimal amount)     { this.amount = amount; }

    public String getDescription()               { return description; }
    public void setDescription(String d)         { this.description = d; }

    public LocalDate getTransactionDate()                  { return transactionDate; }
    public void setTransactionDate(LocalDate d)            { this.transactionDate = d; }

    public LocalDateTime getCreatedAt()                    { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                    { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)      { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format(
            "Transaction{id=%d, type=%s, amount=%s, category='%s', date=%s, description='%s'}",
            id, type, amount, categoryName, transactionDate, description
        );
    }
}
