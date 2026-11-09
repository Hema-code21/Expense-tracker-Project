package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a monthly spending budget set by a user.
 *
 * <p>Each user may have at most one budget per calendar month (enforced by
 * a unique database constraint on {@code (user_id, month, year)}).</p>
 */
public class Budget {

    private int id;
    private int userId;
    private int month;           // 1 = January … 12 = December
    private int year;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Budget() {}

    /** Constructor for creating a new budget (no id yet). */
    public Budget(int userId, int month, int year, BigDecimal amount) {
        this.userId = userId;
        this.month  = month;
        this.year   = year;
        this.amount = amount;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }

    public int getUserId()                       { return userId; }
    public void setUserId(int userId)            { this.userId = userId; }

    public int getMonth()                        { return month; }
    public void setMonth(int month)              { this.month = month; }

    public int getYear()                         { return year; }
    public void setYear(int year)                { this.year = year; }

    public BigDecimal getAmount()                { return amount; }
    public void setAmount(BigDecimal amount)     { this.amount = amount; }

    public LocalDateTime getCreatedAt()                    { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                    { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)      { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("Budget{id=%d, userId=%d, month=%d, year=%d, amount=%s}",
                id, userId, month, year, amount);
    }
}
