-- Development reset (drops existing tables to avoid schema drift)
SET FOREIGN_KEY_CHECKS=0;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS categories;
SET FOREIGN_KEY_CHECKS=1;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NULL
);

-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type ENUM('cash','bank','credit_card') NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    INDEX ix_accounts_user (user_id)
);

-- Categories lookup
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) DEFAULT 'expense',
    color VARCHAR(20)
);

-- Expenses table (with ownership and normalized link columns)
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    expense_date DATE NOT NULL,
    notes TEXT,
    user_id BIGINT NULL DEFAULT 1,
    account_id BIGINT NULL DEFAULT 1,
    category_id BIGINT NULL,
    KEY ix_expenses_user_date (user_id, expense_date),
    KEY ix_expenses_account_date (account_id, expense_date),
    KEY ix_expenses_category_date (category_id, expense_date)
);

