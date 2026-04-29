# Expense Tracker Web Application

A full-stack Expense Tracker built with **Spring Boot, MySQL, and Thymeleaf**.

This application allows users to:
- Add, edit, delete, and search expenses
- Categorize expenses
- Track budgets
- View reports with charts and analytics

---

## 🚀 Features

### Core Features
- Add new expense records
- Edit and update existing expenses
- Delete unwanted or incorrect records
- View all expenses in a table
- Search and filter expenses

### Budget Management
- Set budget limits per category
- Track spending vs budget
- See remaining balance
- Get "Over Budget" alerts

### Reports & Analytics
- Total spending summary
- Average expense calculation
- Top category detection
- Monthly spending trends
- Interactive charts (Chart.js)

---

## 🧱 Technologies Used

- **Java 17**
- **Spring Boot**
- **Thymeleaf (HTML templates)**
- **MySQL**
- **JDBC (JdbcTemplate)**
- **Chart.js (for visualization)**
- **CSS (custom styling)**

---

## 🗄️ Database Schema

### Entities

- Users
- Categories
- Expenses
- Budgets

### Relationships

- One-to-many: Users → Expenses
- One-to-many: Categories → Expenses
- One-to-many: Users → Budgets
- One-to-many: Categories → Budgets

---

## ⚙️ Setup Instructions

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd expense_tracker