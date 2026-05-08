-- =============================================================
-- create_schema.sql
-- ExpenseTrack Database Schema
--
-- Purpose : Creates the expense_tracker database and all tables
--           with primary keys, foreign keys, and constraints.
-- Usage   : mysql -u root < database/create_schema.sql
-- Order   : Run this script BEFORE initialize_data.sql
-- =============================================================

-- Drop and recreate the database for a clean slate
DROP DATABASE IF EXISTS expense_tracker;
CREATE DATABASE expense_tracker;
USE expense_tracker;

-- -------------------------------------------------------------
-- Table: Users
-- Stores application accounts. Role controls data visibility:
--   USER  -> can only see/edit their own records
--   ADMIN -> can see and manage all records
-- -------------------------------------------------------------
CREATE TABLE Users (
    user_id  INT          AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(150) NOT NULL UNIQUE,   -- enforces one account per email
    password VARCHAR(255) NOT NULL,
    role     ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER'
);

-- -------------------------------------------------------------
-- Table: Categories
-- Lookup table for expense and budget categorization.
-- Shared across all users (global list managed by admin).
-- -------------------------------------------------------------
CREATE TABLE Categories (
    category_id   INT          AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL
);

-- -------------------------------------------------------------
-- Table: Expenses
-- Each row is one expense transaction recorded by a user.
-- Foreign keys use ON DELETE CASCADE so that deleting a user
-- or category automatically removes their associated expenses.
-- -------------------------------------------------------------
CREATE TABLE Expenses (
    expense_id     INT            AUTO_INCREMENT PRIMARY KEY,
    user_id        INT            NOT NULL,
    category_id    INT            NOT NULL,
    expense_name   VARCHAR(150)   NOT NULL,
    amount         DECIMAL(10,2)  NOT NULL,
    date           DATE           NOT NULL,
    payment_method VARCHAR(50)    NOT NULL,
    notes          VARCHAR(255)   NOT NULL DEFAULT '',

    -- Referential integrity: deleting a user removes their expenses
    FOREIGN KEY (user_id)     REFERENCES Users(user_id)      ON DELETE CASCADE,
    -- Referential integrity: deleting a category removes expenses in it
    FOREIGN KEY (category_id) REFERENCES Categories(category_id) ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- Table: Budgets
-- Each row is a spending limit set by a user for a category
-- over a date range. The application calculates spent vs limit
-- at query time by summing matching Expenses rows.
-- -------------------------------------------------------------
CREATE TABLE Budgets (
    budget_id    INT           AUTO_INCREMENT PRIMARY KEY,
    user_id      INT           NOT NULL,
    category_id  INT           NOT NULL,
    limit_amount DECIMAL(10,2) NOT NULL,
    start_date   DATE          NOT NULL,
    end_date     DATE          NOT NULL,

    FOREIGN KEY (user_id)     REFERENCES Users(user_id)          ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES Categories(category_id) ON DELETE CASCADE
);
