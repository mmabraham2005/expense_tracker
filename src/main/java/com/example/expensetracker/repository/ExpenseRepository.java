package com.example.expensetracker.repository;

import com.example.expensetracker.model.CategoryTotal;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.ExpenseView;
import com.example.expensetracker.model.MonthlyTotal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ExpenseRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addExpense(Expense expense) {
        String sql = """
            INSERT INTO Expenses (user_id, category_id, expense_name, amount, date, payment_method, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                expense.userId(),
                expense.categoryId(),
                expense.expenseName(),
                expense.amount(),
                expense.date(),
                expense.paymentMethod(),
                expense.notes()
        );
    }

    public List<ExpenseView> findAllExpenseViews() {
        String sql = """
            SELECT e.expense_id,
                   u.name AS user_name,
                   c.category_name,
                   e.expense_name,
                   e.amount,
                   e.date,
                   e.payment_method,
                   e.notes
            FROM Expenses e
            JOIN Users u ON e.user_id = u.user_id
            JOIN Categories c ON e.category_id = c.category_id
            ORDER BY e.expense_id ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new ExpenseView(
                        rs.getInt("expense_id"),
                        rs.getString("user_name"),
                        rs.getString("category_name"),
                        rs.getString("expense_name"),
                        rs.getBigDecimal("amount"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("payment_method"),
                        rs.getString("notes")
                )
        );
    }

    public Expense findExpenseById(Integer expenseId) {
        String sql = """
            SELECT expense_id, user_id, category_id, expense_name, amount, date, payment_method, notes
            FROM Expenses
            WHERE expense_id = ?
        """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                        new Expense(
                                rs.getInt("expense_id"),
                                rs.getInt("user_id"),
                                rs.getInt("category_id"),
                                rs.getString("expense_name"),
                                rs.getBigDecimal("amount"),
                                rs.getDate("date").toLocalDate(),
                                rs.getString("payment_method"),
                                rs.getString("notes")
                        ),
                expenseId
        );
    }

    public void updateExpense(Expense expense) {
        String sql = """
            UPDATE Expenses
            SET user_id = ?, category_id = ?, expense_name = ?, amount = ?, date = ?, payment_method = ?, notes = ?
            WHERE expense_id = ?
        """;

        jdbcTemplate.update(
                sql,
                expense.userId(),
                expense.categoryId(),
                expense.expenseName(),
                expense.amount(),
                expense.date(),
                expense.paymentMethod(),
                expense.notes(),
                expense.expenseId()
        );
    }

    public void deleteExpense(Integer expenseId) {
        String sql = "DELETE FROM Expenses WHERE expense_id = ?";
        jdbcTemplate.update(sql, expenseId);
    }

    public List<ExpenseView> searchExpenses(String keyword, Integer categoryId, String fromDate, String toDate) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.expense_id,
                   u.name AS user_name,
                   c.category_name,
                   e.expense_name,
                   e.amount,
                   e.date,
                   e.payment_method,
                   e.notes
            FROM Expenses e
            JOIN Users u ON e.user_id = u.user_id
            JOIN Categories c ON e.category_id = c.category_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                AND (
                    e.expense_name LIKE ?
                    OR c.category_name LIKE ?
                    OR e.payment_method LIKE ?
                    OR COALESCE(e.notes, '') LIKE ?
                    OR u.name LIKE ?
                )
            """);
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (categoryId != null) {
            sql.append(" AND e.category_id = ? ");
            params.add(categoryId);
        }

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND e.date >= ? ");
            params.add(fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND e.date <= ? ");
            params.add(toDate);
        }

        sql.append(" ORDER BY e.expense_id ASC ");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) ->
                new ExpenseView(
                        rs.getInt("expense_id"),
                        rs.getString("user_name"),
                        rs.getString("category_name"),
                        rs.getString("expense_name"),
                        rs.getBigDecimal("amount"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("payment_method"),
                        rs.getString("notes")
                )
        );
    }

    public BigDecimal getTotalSpending(String fromDate, String toDate, Integer categoryId) {
        StringBuilder sql = new StringBuilder("""
            SELECT COALESCE(SUM(amount), 0)
            FROM Expenses
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND date >= ? ");
            params.add(fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND date <= ? ");
            params.add(toDate);
        }

        if (categoryId != null) {
            sql.append(" AND category_id = ? ");
            params.add(categoryId);
        }

        BigDecimal total = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    public Integer getTransactionCount(String fromDate, String toDate, Integer categoryId) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM Expenses
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND date >= ? ");
            params.add(fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND date <= ? ");
            params.add(toDate);
        }

        if (categoryId != null) {
            sql.append(" AND category_id = ? ");
            params.add(categoryId);
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Integer.class);
        return count == null ? 0 : count;
    }

    public List<CategoryTotal> getCategoryTotals(String fromDate, String toDate, Integer categoryId) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.category_name,
                   COALESCE(SUM(e.amount), 0) AS total
            FROM Categories c
            LEFT JOIN Expenses e ON c.category_id = e.category_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND (e.date IS NULL OR e.date >= ?) ");
            params.add(fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND (e.date IS NULL OR e.date <= ?) ");
            params.add(toDate);
        }

        if (categoryId != null) {
            sql.append(" AND c.category_id = ? ");
            params.add(categoryId);
        }

        sql.append("""
            GROUP BY c.category_id, c.category_name
            ORDER BY total DESC, c.category_name
        """);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) ->
                new CategoryTotal(
                        rs.getString("category_name"),
                        rs.getBigDecimal("total")
                )
        );
    }

    public List<MonthlyTotal> getMonthlyTotals(String fromDate, String toDate, Integer categoryId) {
        StringBuilder sql = new StringBuilder("""
            SELECT DATE_FORMAT(date, '%Y-%m') AS month_label,
                   COALESCE(SUM(amount), 0) AS total
            FROM Expenses
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND date >= ? ");
            params.add(fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND date <= ? ");
            params.add(toDate);
        }

        if (categoryId != null) {
            sql.append(" AND category_id = ? ");
            params.add(categoryId);
        }

        sql.append("""
            GROUP BY DATE_FORMAT(date, '%Y-%m')
            ORDER BY month_label
        """);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) ->
                new MonthlyTotal(
                        rs.getString("month_label"),
                        rs.getBigDecimal("total")
                )
        );
    }
}