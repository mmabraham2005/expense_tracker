package com.example.expensetracker.repository;

import com.example.expensetracker.model.CategoryTotal;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.ExpenseView;
import com.example.expensetracker.model.MonthlyTotal;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ExpenseRepository — Full CRUD operations, search, and report aggregations
 * for the Expenses table.
 *
 * JDBC approach:
 *   All database access uses Spring's JdbcTemplate, which manages connection
 *   lifecycle through HikariCP pooling. Every query uses parameterized ?
 *   placeholders — user-supplied values are never concatenated into SQL strings,
 *   which eliminates SQL injection vulnerabilities.
 *
 * Dynamic SQL pattern:
 *   Several methods build queries at runtime using StringBuilder + a parallel
 *   List<Object> for bind values. Each optional filter appends " AND col = ?"
 *   and adds the value to the list. "WHERE 1=1" is used as a base predicate so
 *   every optional filter can be appended uniformly as "AND ..." without
 *   special-casing the first condition.
 *
 * Data isolation pattern:
 *   Methods that return expense data accept an Integer userId parameter:
 *     - null  → admin caller: no user filter, all users' records are returned.
 *     - value → regular user: "AND e.user_id = ?" restricts results to that
 *               user's own expenses at the SQL layer (not just in Java).
 *
 * Exception handling:
 *   JdbcTemplate converts checked JDBC SQLExceptions into Spring's unchecked
 *   DataAccessException hierarchy. Each method catches DataAccessException and
 *   rethrows as RuntimeException with context so callers and logs can identify
 *   the failing operation. findExpenseById additionally catches the more specific
 *   EmptyResultDataAccessException (thrown by queryForObject when zero rows
 *   match) and converts it to null so the controller can redirect gracefully.
 */
@Repository
public class ExpenseRepository {

    // JdbcTemplate is injected by Spring and backed by the HikariCP DataSource
    // configured in application.properties.
    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Inserts a new expense record for the logged-in user.
     *
     * The userId on the Expense object comes from the HTTP session (set at login),
     * not from any form field. This prevents a user from submitting an expense
     * under a different user_id by crafting the request manually.
     *
     * SQL:
     *   INSERT INTO Expenses
     *     (user_id, category_id, expense_name, amount, date, payment_method, notes)
     *   VALUES (?, ?, ?, ?, ?, ?, ?)
     *
     * @param expense an Expense record with all fields populated; expenseId is
     *                null because MySQL generates it via AUTO_INCREMENT
     */
    public void addExpense(Expense expense) {
        // All seven columns are always provided — no dynamic SQL needed.
        String sql = """
            INSERT INTO Expenses (user_id, category_id, expense_name, amount, date, payment_method, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try {
            // jdbcTemplate.update() executes INSERT/UPDATE/DELETE statements.
            // Bind values map left-to-right to each ? placeholder in the SQL.
            jdbcTemplate.update(
                    sql,
                    expense.userId(),       // FK → Users.user_id  (from HTTP session)
                    expense.categoryId(),   // FK → Categories.category_id
                    expense.expenseName(),  // VARCHAR description
                    expense.amount(),       // DECIMAL(10,2)
                    expense.date(),         // DATE
                    expense.paymentMethod(),// VARCHAR: e.g. "Credit Card", "Cash"
                    expense.notes()         // VARCHAR default '' — never NULL
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while adding expense: " + e.getMessage(), e);
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * Returns all expenses as display-ready views joined with user and category names.
     *
     * Two INNER JOINs are used:
     *   1. JOIN Users      — to show the owner's name instead of a raw user_id.
     *   2. JOIN Categories — to show the category label instead of a raw category_id.
     * These joins always succeed because both foreign keys have NOT NULL constraints
     * and referential integrity is enforced by ON DELETE CASCADE.
     *
     * SQL:
     *   SELECT e.expense_id,
     *          u.name        AS user_name,
     *          c.category_name,
     *          e.expense_name,
     *          e.amount,
     *          e.date,
     *          e.payment_method,
     *          e.notes
     *   FROM Expenses e
     *   JOIN Users u      ON e.user_id     = u.user_id
     *   JOIN Categories c ON e.category_id = c.category_id
     *   WHERE 1=1
     *   [AND e.user_id = ?]   -- appended when userId != null
     *   ORDER BY e.expense_id ASC
     *
     * @param userId null for admin (returns all), or session userId (own records only)
     */
    public List<ExpenseView> findAllExpenseViews(Integer userId) {
        // --- Step 1: Base SELECT with JOINs for display names ---
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
            JOIN Users u      ON e.user_id     = u.user_id
            JOIN Categories c ON e.category_id = c.category_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        // --- Step 2: Scope filter — regular users see only their own rows ---
        if (userId != null) {
            sql.append(" AND e.user_id = ? ");
            params.add(userId);
        }

        // --- Step 3: Sort by primary key so the list is stable across calls ---
        sql.append(" ORDER BY e.expense_id ASC");

        try {
            // --- Step 4: Execute query and map each row to an ExpenseView record ---
            // jdbcTemplate.query() returns an empty list (not null) when no rows match.
            return jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                    new ExpenseView(
                            rs.getInt("expense_id"),
                            rs.getString("user_name"),
                            rs.getString("category_name"),
                            rs.getString("expense_name"),
                            rs.getBigDecimal("amount"),
                            rs.getDate("date").toLocalDate(), // java.sql.Date → java.time.LocalDate
                            rs.getString("payment_method"),
                            rs.getString("notes")
                    ), params.toArray()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while fetching expenses: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up a single expense row by its primary key.
     *
     * Returns the raw Expense record (not the joined ExpenseView) so the
     * edit and delete controllers can verify the userId field for ownership
     * checking before allowing the operation to proceed.
     *
     * SQL:
     *   SELECT expense_id, user_id, category_id, expense_name,
     *          amount, date, payment_method, notes
     *   FROM Expenses
     *   WHERE expense_id = ?
     *
     * @param expenseId the primary key to look up
     * @return the matching Expense record, or null if no row with that ID exists
     */
    public Expense findExpenseById(Integer expenseId) {
        // Static query — only one bind value (the primary key).
        String sql = """
            SELECT expense_id, user_id, category_id, expense_name,
                   amount, date, payment_method, notes
            FROM Expenses
            WHERE expense_id = ?
        """;

        try {
            // queryForObject() expects exactly one row. It throws:
            //   EmptyResultDataAccessException — if zero rows match (handled below)
            //   IncorrectResultSizeDataAccessException — if more than one row matches
            //     (impossible here since expense_id is the PRIMARY KEY)
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                    new Expense(
                            rs.getInt("expense_id"),
                            rs.getInt("user_id"),       // used for ownership checks in the controller
                            rs.getInt("category_id"),
                            rs.getString("expense_name"),
                            rs.getBigDecimal("amount"),
                            rs.getDate("date").toLocalDate(),
                            rs.getString("payment_method"),
                            rs.getString("notes")
                    ),
                    expenseId
            );
        } catch (EmptyResultDataAccessException e) {
            // No row with this ID — return null so the controller can redirect
            // to the list page instead of crashing with a 500 error.
            return null;
        } catch (DataAccessException e) {
            throw new RuntimeException(
                    "Database error while looking up expense #" + expenseId + ": " + e.getMessage(), e);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Updates all editable fields of an existing expense row.
     *
     * The controller calls findExpenseById() and verifies that the record's
     * userId matches the session before invoking this method, so ownership
     * is enforced before the UPDATE reaches the database.
     *
     * SQL:
     *   UPDATE Expenses
     *   SET user_id        = ?,
     *       category_id    = ?,
     *       expense_name   = ?,
     *       amount         = ?,
     *       date           = ?,
     *       payment_method = ?,
     *       notes          = ?
     *   WHERE expense_id = ?
     *
     * @param expense an Expense record carrying the new field values;
     *                expenseId must be non-null to identify the row to update
     */
    public void updateExpense(Expense expense) {
        // All columns are updated in one statement — no partial update logic needed.
        String sql = """
            UPDATE Expenses
            SET user_id        = ?,
                category_id    = ?,
                expense_name   = ?,
                amount         = ?,
                date           = ?,
                payment_method = ?,
                notes          = ?
            WHERE expense_id = ?
        """;

        try {
            // Note the parameter order: all SET values come before the WHERE value.
            jdbcTemplate.update(
                    sql,
                    expense.userId(),
                    expense.categoryId(),
                    expense.expenseName(),
                    expense.amount(),
                    expense.date(),
                    expense.paymentMethod(),
                    expense.notes(),
                    expense.expenseId()    // bound to the WHERE expense_id = ? clause
            );
        } catch (DataAccessException e) {
            throw new RuntimeException(
                    "Database error while updating expense #" + expense.expenseId() + ": " + e.getMessage(), e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Deletes one expense row by primary key.
     *
     * The controller verifies ownership (expense.userId == session userId)
     * before calling this method, so a regular user cannot delete another
     * user's expense by guessing an expense_id.
     *
     * SQL:
     *   DELETE FROM Expenses
     *   WHERE expense_id = ?
     *
     * @param expenseId the primary key of the expense to remove
     */
    public void deleteExpense(Integer expenseId) {
        // Single-value parameterized DELETE — no dynamic SQL needed.
        String sql = "DELETE FROM Expenses WHERE expense_id = ?";

        try {
            // update() returns the row count; we ignore it here since
            // the controller already verified the row exists.
            jdbcTemplate.update(sql, expenseId);
        } catch (DataAccessException e) {
            throw new RuntimeException(
                    "Database error while deleting expense #" + expenseId + ": " + e.getMessage(), e);
        }
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    /**
     * Full-featured expense search with six independently optional filters.
     * All active filters are combined with AND logic.
     *
     * Dynamic SQL construction:
     *   A StringBuilder accumulates the WHERE clause and a parallel List<Object>
     *   accumulates the bind values. Each filter block appends one "AND col op ?"
     *   fragment and adds the corresponding value to the list. Because every
     *   fragment starts with "AND", the base clause is "WHERE 1=1" — a no-op
     *   predicate that makes the first real filter valid without a special case.
     *
     * Keyword search:
     *   The keyword is wrapped in % wildcards in Java ("%" + keyword + "%") and
     *   passed as a parameterized bind value — never concatenated into the SQL.
     *   CAST(e.amount AS CHAR) lets users find expenses by typing a number like
     *   "50" in the keyword box.
     *
     * Amount range:
     *   minAmount/maxAmount use >= / <= so boundary values are included. Either
     *   bound can be omitted independently (e.g. "all expenses over $100").
     *
     * SQL structure:
     *   SELECT e.expense_id, u.name AS user_name, c.category_name,
     *          e.expense_name, e.amount, e.date, e.payment_method, e.notes
     *   FROM Expenses e
     *   JOIN Users u      ON e.user_id     = u.user_id
     *   JOIN Categories c ON e.category_id = c.category_id
     *   WHERE 1=1
     *   [AND e.user_id = ?]
     *   [AND (e.expense_name LIKE ?
     *         OR c.category_name LIKE ?
     *         OR e.payment_method LIKE ?
     *         OR COALESCE(e.notes, '') LIKE ?
     *         OR u.name LIKE ?
     *         OR CAST(e.amount AS CHAR) LIKE ?)]
     *   [AND e.category_id = ?]
     *   [AND e.date >= ?]
     *   [AND e.date <= ?]
     *   [AND e.amount >= ?]
     *   [AND e.amount <= ?]
     *   ORDER BY e.expense_id ASC
     *
     * @param keyword    optional text matched via LIKE against multiple columns
     * @param categoryId optional exact-match filter on category
     * @param fromDate   optional inclusive lower date bound (YYYY-MM-DD string)
     * @param toDate     optional inclusive upper date bound (YYYY-MM-DD string)
     * @param minAmount  optional inclusive lower amount bound
     * @param maxAmount  optional inclusive upper amount bound
     * @param userId     null for admin (all users), or session userId (own records)
     */
    public List<ExpenseView> searchExpenses(String keyword, Integer categoryId,
                                            String fromDate, String toDate,
                                            BigDecimal minAmount, BigDecimal maxAmount,
                                            Integer userId) {
        // --- Step 1: Base SELECT with JOINs (same structure as findAllExpenseViews) ---
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
            JOIN Users u      ON e.user_id     = u.user_id
            JOIN Categories c ON e.category_id = c.category_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        // --- Step 2: User-scope filter ---
        // Appended first so all subsequent filters apply within the user's own data.
        if (userId != null) {
            sql.append(" AND e.user_id = ? ");
            params.add(userId);
        }

        // --- Step 3: Keyword filter (multi-column LIKE) ---
        if (keyword != null && !keyword.isBlank()) {
            // The OR block matches any row where the keyword appears in at least
            // one of the listed columns. COALESCE(e.notes,'') prevents LIKE from
            // returning null (and thus no match) on rows with empty notes.
            sql.append("""
                AND (
                    e.expense_name          LIKE ?
                    OR c.category_name      LIKE ?
                    OR e.payment_method     LIKE ?
                    OR COALESCE(e.notes,'') LIKE ?
                    OR u.name               LIKE ?
                    OR CAST(e.amount AS CHAR) LIKE ?
                )
            """);
            // % wildcards are added in Java, not in the SQL, so the value
            // is still treated as a parameterized bind — not concatenated SQL.
            String like = "%" + keyword.trim() + "%";
            // Each ? in the OR block gets its own bind value (six total).
            params.add(like); // expense_name
            params.add(like); // category_name
            params.add(like); // payment_method
            params.add(like); // notes
            params.add(like); // user name
            params.add(like); // amount as string
        }

        // --- Step 4: Exact category filter ---
        if (categoryId != null) {
            sql.append(" AND e.category_id = ? ");
            params.add(categoryId);
        }

        // --- Step 5: Date range filter (inclusive on both ends) ---
        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND e.date >= ? ");
            params.add(fromDate); // MySQL parses 'YYYY-MM-DD' strings automatically
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND e.date <= ? ");
            params.add(toDate);
        }

        // --- Step 6: Amount range filter (inclusive on both ends) ---
        if (minAmount != null) {
            sql.append(" AND e.amount >= ? ");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            sql.append(" AND e.amount <= ? ");
            params.add(maxAmount);
        }

        // --- Step 7: Stable ordering ---
        sql.append(" ORDER BY e.expense_id ASC ");

        try {
            // --- Step 8: Execute the fully assembled query ---
            // params.toArray() produces the Object[] that JdbcTemplate binds to the ?s.
            return jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                    new ExpenseView(
                            rs.getInt("expense_id"),
                            rs.getString("user_name"),
                            rs.getString("category_name"),
                            rs.getString("expense_name"),
                            rs.getBigDecimal("amount"),
                            rs.getDate("date").toLocalDate(),
                            rs.getString("payment_method"),
                            rs.getString("notes")
                    ), params.toArray()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error during expense search: " + e.getMessage(), e);
        }
    }

    // ── REPORT AGGREGATIONS ───────────────────────────────────────────────────

    /**
     * Returns the SUM of expense amounts matching the given filters.
     *
     * COALESCE(SUM(amount), 0) is critical: SUM returns NULL when no rows match
     * (e.g. no expenses in the selected date range). COALESCE converts that NULL
     * to 0 so the Reports page always displays a numeric value.
     *
     * SQL:
     *   SELECT COALESCE(SUM(amount), 0)
     *   FROM Expenses
     *   WHERE 1=1
     *   [AND user_id     = ?]
     *   [AND date        >= ?]
     *   [AND date        <= ?]
     *   [AND category_id = ?]
     *
     * @param fromDate   optional inclusive start date
     * @param toDate     optional inclusive end date
     * @param categoryId optional category filter
     * @param userId     null for admin (all users), or session userId
     * @return total spending as BigDecimal, never null
     */
    public BigDecimal getTotalSpending(String fromDate, String toDate,
                                       Integer categoryId, Integer userId) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(amount), 0) FROM Expenses WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // Append each active filter as a parameterized AND clause.
        if (userId     != null)                       { sql.append(" AND user_id     = ?"); params.add(userId); }
        if (fromDate   != null && !fromDate.isBlank()) { sql.append(" AND date        >= ?"); params.add(fromDate); }
        if (toDate     != null && !toDate.isBlank())   { sql.append(" AND date        <= ?"); params.add(toDate); }
        if (categoryId != null)                        { sql.append(" AND category_id = ?"); params.add(categoryId); }

        try {
            // queryForObject maps the single returned value to BigDecimal.
            BigDecimal total = jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, params.toArray());
            // Extra null guard in case COALESCE fails or the connection drops mid-query.
            return total == null ? BigDecimal.ZERO : total;
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while calculating total spending: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the COUNT of expense rows matching the given filters.
     * Used to compute the transaction count and derive the average expense
     * on the Reports page (average = total / count, calculated in the controller).
     *
     * SQL:
     *   SELECT COUNT(*)
     *   FROM Expenses
     *   WHERE 1=1
     *   [AND user_id     = ?]
     *   [AND date        >= ?]
     *   [AND date        <= ?]
     *   [AND category_id = ?]
     *
     * @param fromDate   optional inclusive start date
     * @param toDate     optional inclusive end date
     * @param categoryId optional category filter
     * @param userId     null for admin, or session userId
     * @return transaction count, never null
     */
    public Integer getTransactionCount(String fromDate, String toDate,
                                        Integer categoryId, Integer userId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Expenses WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (userId     != null)                       { sql.append(" AND user_id     = ?"); params.add(userId); }
        if (fromDate   != null && !fromDate.isBlank()) { sql.append(" AND date        >= ?"); params.add(fromDate); }
        if (toDate     != null && !toDate.isBlank())   { sql.append(" AND date        <= ?"); params.add(toDate); }
        if (categoryId != null)                        { sql.append(" AND category_id = ?"); params.add(categoryId); }

        try {
            Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
            return count == null ? 0 : count;
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while counting transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Returns total spending grouped by category, sorted by total descending.
     *
     * LEFT JOIN design rationale:
     *   Using LEFT JOIN (instead of INNER JOIN) means every category row is
     *   included in the result even if no matching expenses exist — COALESCE
     *   gives those categories a total of 0. This is important when a specific
     *   category is selected in the filter: an INNER JOIN would return zero rows
     *   and the chart would appear blank.
     *
     *   Expense-level filters (user_id, date range) must go in the JOIN ON clause,
     *   NOT in the WHERE clause. Putting them in WHERE would silently convert the
     *   LEFT JOIN into an INNER JOIN, hiding categories with no matching expenses.
     *   The category filter targets the Categories table directly and belongs in WHERE.
     *
     * SQL:
     *   SELECT c.category_name,
     *          COALESCE(SUM(e.amount), 0) AS total
     *   FROM Categories c
     *   LEFT JOIN Expenses e ON c.category_id = e.category_id
     *                       [AND e.user_id  = ?]
     *                       [AND e.date    >= ?]
     *                       [AND e.date    <= ?]
     *   WHERE 1=1
     *   [AND c.category_id = ?]
     *   GROUP BY c.category_id, c.category_name
     *   ORDER BY total DESC, c.category_name
     *
     * @param fromDate   optional inclusive start date (applied in JOIN ON)
     * @param toDate     optional inclusive end date   (applied in JOIN ON)
     * @param categoryId optional filter to a single category (applied in WHERE)
     * @param userId     null for admin (all users), or session userId (JOIN ON)
     */
    public List<CategoryTotal> getCategoryTotals(String fromDate, String toDate,
                                                  Integer categoryId, Integer userId) {
        // --- Step 1: Build the JOIN ON condition ---
        // Expense-level filters go here to preserve LEFT JOIN semantics.
        StringBuilder joinOn = new StringBuilder(" ON c.category_id = e.category_id");
        List<Object> params  = new ArrayList<>();

        if (userId   != null)                        { joinOn.append(" AND e.user_id = ?"); params.add(userId); }
        if (fromDate != null && !fromDate.isBlank()) { joinOn.append(" AND e.date   >= ?"); params.add(fromDate); }
        if (toDate   != null && !toDate.isBlank())   { joinOn.append(" AND e.date   <= ?"); params.add(toDate); }

        // --- Step 2: Assemble the full query ---
        StringBuilder sql = new StringBuilder(
            "SELECT c.category_name, COALESCE(SUM(e.amount), 0) AS total " +
            "FROM Categories c LEFT JOIN Expenses e"
        );
        sql.append(joinOn);
        sql.append(" WHERE 1=1");

        // Category filter targets Categories directly — safe to put in WHERE.
        if (categoryId != null) {
            sql.append(" AND c.category_id = ?");
            params.add(categoryId);
        }

        // GROUP BY required for the SUM aggregate; ORDER BY shows top spenders first.
        sql.append(" GROUP BY c.category_id, c.category_name ORDER BY total DESC, c.category_name");

        try {
            // --- Step 3: Execute and map to CategoryTotal records ---
            return jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                    new CategoryTotal(
                            rs.getString("category_name"),
                            rs.getBigDecimal("total")   // 0.00 for categories with no expenses
                    ), params.toArray()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while fetching category totals: " + e.getMessage(), e);
        }
    }

    /**
     * Returns total spending grouped by calendar month (YYYY-MM format),
     * sorted chronologically. Consumed by the Reports bar chart.
     *
     * DATE_FORMAT(date, '%Y-%m') groups all expenses in the same month together
     * regardless of the day. This produces labels like "2026-01", "2026-02" that
     * the Chart.js chart uses as x-axis tick labels.
     *
     * SQL:
     *   SELECT DATE_FORMAT(date, '%Y-%m') AS month_label,
     *          COALESCE(SUM(amount), 0)   AS total
     *   FROM Expenses
     *   WHERE 1=1
     *   [AND user_id     = ?]
     *   [AND date        >= ?]
     *   [AND date        <= ?]
     *   [AND category_id = ?]
     *   GROUP BY DATE_FORMAT(date, '%Y-%m')
     *   ORDER BY month_label
     *
     * @param fromDate   optional inclusive start date
     * @param toDate     optional inclusive end date
     * @param categoryId optional category filter
     * @param userId     null for admin, or session userId
     */
    public List<MonthlyTotal> getMonthlyTotals(String fromDate, String toDate,
                                                Integer categoryId, Integer userId) {
        StringBuilder sql = new StringBuilder("""
            SELECT DATE_FORMAT(date, '%Y-%m') AS month_label,
                   COALESCE(SUM(amount), 0)   AS total
            FROM Expenses
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        // Append optional filters — same pattern as getTotalSpending and getTransactionCount.
        if (userId     != null)                       { sql.append(" AND user_id     = ?"); params.add(userId); }
        if (fromDate   != null && !fromDate.isBlank()) { sql.append(" AND date        >= ?"); params.add(fromDate); }
        if (toDate     != null && !toDate.isBlank())   { sql.append(" AND date        <= ?"); params.add(toDate); }
        if (categoryId != null)                        { sql.append(" AND category_id = ?"); params.add(categoryId); }

        // GROUP BY the formatted month string; ORDER BY ensures chronological x-axis.
        sql.append(" GROUP BY DATE_FORMAT(date, '%Y-%m') ORDER BY month_label");

        try {
            return jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                    new MonthlyTotal(
                            rs.getString("month_label"), // e.g. "2026-01"
                            rs.getBigDecimal("total")    // sum of all expenses that month
                    ), params.toArray()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while fetching monthly totals: " + e.getMessage(), e);
        }
    }
}
