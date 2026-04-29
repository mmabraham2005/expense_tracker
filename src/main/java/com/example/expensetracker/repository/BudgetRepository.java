package com.example.expensetracker.repository;

import com.example.expensetracker.model.Budget;
import com.example.expensetracker.model.BudgetView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class BudgetRepository {

    private final JdbcTemplate jdbcTemplate;

    public BudgetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addBudget(Budget budget) {
        String sql = """
            INSERT INTO Budgets (user_id, category_id, limit_amount, start_date, end_date)
            VALUES (?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                budget.userId(),
                budget.categoryId(),
                budget.limitAmount(),
                budget.startDate(),
                budget.endDate()
        );
    }

    public List<Budget> findAllBudgets() {
        String sql = "SELECT * FROM Budgets ORDER BY budget_id ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new Budget(
                        rs.getInt("budget_id"),
                        rs.getInt("user_id"),
                        rs.getInt("category_id"),
                        rs.getBigDecimal("limit_amount"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate()
                )
        );
    }

    public List<BudgetView> findAllBudgetViews() {
        String sql = """
            SELECT
                b.budget_id,
                u.name AS user_name,
                c.category_name,
                b.limit_amount,
                b.start_date,
                b.end_date,
                COALESCE(SUM(e.amount), 0) AS spent_amount
            FROM Budgets b
            JOIN Users u ON b.user_id = u.user_id
            JOIN Categories c ON b.category_id = c.category_id
            LEFT JOIN Expenses e
                ON e.user_id = b.user_id
                AND e.category_id = b.category_id
                AND e.date BETWEEN b.start_date AND b.end_date
            GROUP BY
                b.budget_id,
                u.name,
                c.category_name,
                b.limit_amount,
                b.start_date,
                b.end_date
            ORDER BY b.budget_id ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal limitAmount = rs.getBigDecimal("limit_amount");
            BigDecimal spentAmount = rs.getBigDecimal("spent_amount");

            if (limitAmount == null) {
                limitAmount = BigDecimal.ZERO;
            }
            if (spentAmount == null) {
                spentAmount = BigDecimal.ZERO;
            }

            BigDecimal remainingAmount = limitAmount.subtract(spentAmount);
            String status = remainingAmount.compareTo(BigDecimal.ZERO) < 0 ? "Over Budget" : "Within Budget";

            return new BudgetView(
                    rs.getInt("budget_id"),
                    rs.getString("user_name"),
                    rs.getString("category_name"),
                    limitAmount,
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate(),
                    spentAmount,
                    remainingAmount,
                    status
            );
        });
    }
}