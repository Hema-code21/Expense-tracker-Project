package com.expensetracker.model;

import java.time.LocalDateTime;

/**
 * Represents a registered application user.
 *
 * <p>Passwords are stored as BCrypt hashes — never plain text.</p>
 */
public class User {

    private int id;
    private String username;
    private String email;
    private String password;          // BCrypt hash
    private LocalDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public User() {}

    /** Constructor used when registering a new user (id not yet known). */
    public User(String username, String email, String password) {
        this.username  = username;
        this.email     = email;
        this.password  = password;
    }

    /** Full constructor used when loading from the database. */
    public User(int id, String username, String email, String password, LocalDateTime createdAt) {
        this.id        = id;
        this.username  = username;
        this.email     = email;
        this.password  = password;
        this.createdAt = createdAt;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getUsername()             { return username; }
    public void setUsername(String u)       { this.username = u; }

    public String getEmail()                { return email; }
    public void setEmail(String e)          { this.email = e; }

    public String getPassword()             { return password; }
    public void setPassword(String p)       { this.password = p; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
}
