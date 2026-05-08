package com.example.expensetracker.repository;

import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LookupRepository — User authentication, registration, and category lookup.
 *
 * JDBC approach:
 *   All database access goes through Spring's JdbcTemplate, which manages
 *   connection acquisition and release via HikariCP connection pooling.
 *   Every query uses parameterized placeholders (?) — never string
 *   concatenation — to prevent SQL injection attacks.
 *
 * Exception handling:
 *   JdbcTemplate converts all checked JDBC SQLExceptions into Spring's
 *   unchecked DataAccessException hierarchy. Each method catches
 *   DataAccessException and rethrows as RuntimeException with a
 *   descriptive message so the caller and logs clearly identify the
 *   failing operation.
 *
 * Password security:
 *   BCryptPasswordEncoder (cost factor 10) is used for both hashing new
 *   passwords and verifying login attempts. Passwords are never stored
 *   or compared in plain text.
 */
@Repository
public class LookupRepository {

    // JdbcTemplate wraps a DataSource and provides type-safe query helpers.
    // Spring injects the configured DataSource automatically via the constructor.
    private final JdbcTemplate jdbcTemplate;

    // BCryptPasswordEncoder is stateless and thread-safe; one instance is sufficient.
    // Default cost factor is 10 (2^10 = 1024 hashing rounds).
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── READ: Users ───────────────────────────────────────────────────────────

    /**
     * Returns all registered users ordered alphabetically by name.
     * Used exclusively by the admin User Management dashboard.
     *
     * Note: the password column is intentionally excluded from this SELECT
     * so BCrypt hashes never appear in a Thymeleaf model attribute, reducing
     * the risk of accidental exposure in logs or stack traces.
     *
     * SQL:
     *   SELECT user_id, name, email, role
     *   FROM Users
     *   ORDER BY name
     *
     * @return list of all users (password field is null on each record)
     */
    public List<User> findAllUsers() {
        // Static query — no filters, so no StringBuilder needed.
        String sql = "SELECT user_id, name, email, role FROM Users ORDER BY name";

        try {
            // jdbcTemplate.query maps each ResultSet row to a User record via a lambda row mapper.
            // The lambda receives the ResultSet (rs) and the current row number (rowNum).
            return jdbcTemplate.query(sql, (rs, rowNum) ->
                new User(
                    rs.getInt("user_id"),      // INT primary key
                    rs.getString("name"),       // VARCHAR display name
                    rs.getString("email"),      // VARCHAR unique email
                    null,                       // password intentionally omitted
                    rs.getString("role")        // ENUM: 'USER' or 'ADMIN'
                )
            );
        } catch (DataAccessException e) {
            // Wrap Spring's checked exception with context so the caller knows which query failed.
            throw new RuntimeException("Database error while fetching user list: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticates a user by email address and plain-text password.
     *
     * Strategy: fetch the user row by email first (indexed lookup), then use
     * BCrypt's encoder.matches() to compare the submitted password against the
     * stored hash. This approach never sends a password to the database —
     * the comparison happens entirely in Java.
     *
     * SQL:
     *   SELECT user_id, name, email, password, role
     *   FROM Users
     *   WHERE email = ?          -- parameterized: prevents SQL injection
     *
     * @param email    the submitted email address (used as the lookup key)
     * @param password the submitted plain-text password (verified in Java via BCrypt)
     * @return the authenticated User record, or null if credentials are invalid
     */
    public User findByEmailAndPassword(String email, String password) {
        // Fetch by email only — the WHERE clause uses a parameterized ? placeholder.
        // Using a List (not queryForObject) avoids EmptyResultDataAccessException
        // when the email doesn't exist; we handle the empty case ourselves below.
        String sql = "SELECT user_id, name, email, password, role FROM Users WHERE email = ?";

        try {
            // Pass 'email' as the single bind value for the ? placeholder.
            List<User> users = jdbcTemplate.query(sql, (rs, rowNum) ->
                new User(
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password"),  // BCrypt hash stored in the DB
                    rs.getString("role")
                ), email
            );

            // No user found with that email address.
            if (users.isEmpty()) return null;

            User user = users.get(0);

            // BCrypt check: encoder.matches(rawInput, storedHash).
            // Returns true only if the plain-text password hashes to the stored value.
            // This is a constant-time comparison that prevents timing attacks.
            return encoder.matches(password, user.password()) ? user : null;

        } catch (DataAccessException e) {
            throw new RuntimeException("Database error during user authentication: " + e.getMessage(), e);
        }
    }

    // ── CREATE: Users ─────────────────────────────────────────────────────────

    /**
     * Checks whether an account with the given email already exists.
     * Called during registration to enforce the UNIQUE constraint on Users.email
     * before attempting an INSERT (which would throw a DataIntegrityViolationException).
     *
     * SQL:
     *   SELECT COUNT(*)
     *   FROM Users
     *   WHERE email = ?          -- parameterized: prevents SQL injection
     *
     * @param email the email address to look up (case-insensitive matching is
     *              handled by normalising to lowercase before the call)
     * @return true if at least one row matches, false otherwise
     */
    public boolean existsByEmail(String email) {
        // COUNT(*) always returns exactly one row, so queryForObject is appropriate here.
        String sql = "SELECT COUNT(*) FROM Users WHERE email = ?";

        try {
            // queryForObject(sql, requiredType, args...) — maps a single-value result to Integer.
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);

            // COUNT(*) can theoretically return null if the connection drops mid-query;
            // treat null as 0 to fail safely (allow the registration to proceed).
            return count != null && count > 0;

        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while checking email uniqueness: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a new user account with the role USER.
     *
     * The raw password is BCrypt-hashed before the INSERT — the plain-text value
     * never touches the database. The email is normalised to lowercase so
     * lookups are case-insensitive without requiring a case-insensitive collation.
     *
     * SQL:
     *   INSERT INTO Users (name, email, password, role)
     *   VALUES (?, ?, ?, 'USER')
     *       -- role is hardcoded to 'USER'; admin accounts cannot be
     *       -- self-registered through the public sign-up form.
     *
     * @param name        the user's display name (trimmed of leading/trailing whitespace)
     * @param email       the user's email address (lowercased and trimmed)
     * @param rawPassword the plain-text password chosen by the user (hashed before storage)
     */
    public void registerUser(String name, String email, String rawPassword) {
        // Hash the password with BCrypt before building the SQL.
        // encoder.encode() generates a unique salt internally, so two calls
        // with the same input produce different hashes — both verifiable via matches().
        String hashed = encoder.encode(rawPassword);

        // Role is hardcoded to 'USER' — admin access cannot be self-granted.
        String sql = "INSERT INTO Users (name, email, password, role) VALUES (?, ?, ?, 'USER')";

        try {
            // jdbcTemplate.update() is used for INSERT, UPDATE, and DELETE statements.
            // Bind values are positional — they map left-to-right to each ? placeholder.
            jdbcTemplate.update(sql, name.trim(), email.trim().toLowerCase(), hashed);

        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while registering user: " + e.getMessage(), e);
        }
    }

    // ── READ: Categories ──────────────────────────────────────────────────────

    /**
     * Returns all expense categories sorted alphabetically.
     * Categories are a global lookup table shared by every user; they are used
     * to populate dropdown menus on the Add Expense, Edit Expense, Search, and
     * Budget forms.
     *
     * SQL:
     *   SELECT category_id, category_name
     *   FROM Categories
     *   ORDER BY category_name
     *
     * @return complete list of categories ordered A–Z
     */
    public List<Category> findAllCategories() {
        String sql = "SELECT category_id, category_name FROM Categories ORDER BY category_name";

        try {
            // Row mapper converts each result row into a Category record.
            return jdbcTemplate.query(sql, (rs, rowNum) ->
                new Category(
                    rs.getInt("category_id"),       // INT primary key
                    rs.getString("category_name")   // VARCHAR display name
                )
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while fetching category list: " + e.getMessage(), e);
        }
    }
}
