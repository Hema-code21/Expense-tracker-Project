-- =============================================================
-- Expense Tracker Database Schema
-- MySQL 8.0+
-- Run this file ONCE to initialize the database:
--   mysql -u root -p < schema.sql
-- =============================================================

-- Create and select the database
CREATE DATABASE IF NOT EXISTS expense_tracker
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE expense_tracker;

-- =============================================================
-- Table: users
-- Stores registered application users.
-- =============================================================
CREATE TABLE IF NOT EXISTS users (
    id         INT          NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    email      VARCHAR(150) NOT NULL,
    password   VARCHAR(255) NOT NULL,   -- BCrypt hash
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- Table: categories
-- Predefined transaction categories (shared across all users).
-- =============================================================
CREATE TABLE IF NOT EXISTS categories (
    id   INT         NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed predefined categories
INSERT IGNORE INTO categories (name) VALUES
    ('Food'),
    ('Travel'),
    ('Rent'),
    ('Shopping'),
    ('Bills'),
    ('Salary'),
    ('Other');

-- =============================================================
-- Table: transactions
-- Records every income or expense entry for a user.
-- =============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id              INT            NOT NULL AUTO_INCREMENT,
    user_id         INT            NOT NULL,
    category_id     INT            NOT NULL,
    type            ENUM('INCOME','EXPENSE') NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    description     VARCHAR(255)   NOT NULL,
    transaction_date DATE           NOT NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_txn_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_txn_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,

    -- Indexes for frequently filtered columns
    INDEX idx_txn_user_id         (user_id),
    INDEX idx_txn_category_id     (category_id),
    INDEX idx_txn_type            (type),
    INDEX idx_txn_transaction_date (transaction_date),
    INDEX idx_txn_description     (description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- Table: budgets
-- Monthly budget set by each user.
-- One budget per user per year-month.
-- =============================================================
CREATE TABLE IF NOT EXISTS budgets (
    id         INT            NOT NULL AUTO_INCREMENT,
    user_id    INT            NOT NULL,
    month      TINYINT        NOT NULL COMMENT '1=January … 12=December',
    year       SMALLINT       NOT NULL,
    amount     DECIMAL(12, 2) NOT NULL,
    created_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- A user can have at most one budget per month/year
    UNIQUE KEY uq_user_month_year (user_id, month, year),

    INDEX idx_budget_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
