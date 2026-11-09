package com.expensetracker.model;

/**
 * Represents a transaction category (e.g., Food, Travel, Salary).
 *
 * <p>Categories are predefined and shared across all users.</p>
 */
public class Category {

    private int id;
    private String name;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Category() {}

    public Category(int id, String name) {
        this.id   = id;
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int getId()             { return id; }
    public void setId(int id)      { this.id = id; }

    public String getName()        { return name; }
    public void setName(String n)  { this.name = n; }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "'}";
    }
}
