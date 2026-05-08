package com.example.expensetracker.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Runs once on startup and hashes any plain-text passwords still in the database.
 * BCrypt hashes always start with "$2a$", "$2b$", or "$2y$", so any password
 * that does not match that prefix is treated as plain text and upgraded in place.
 * This is a one-time migration — after the first run every password is a BCrypt hash
 * and this runner becomes a no-op.
 */
@Component
public class PasswordMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public PasswordMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> users =
                jdbcTemplate.queryForList("SELECT user_id, password FROM Users");

        for (Map<String, Object> row : users) {
            String stored = (String) row.get("password");
            // Already a BCrypt hash — skip
            if (stored != null && stored.startsWith("$2")) continue;

            String hashed = encoder.encode(stored);
            jdbcTemplate.update(
                    "UPDATE Users SET password = ? WHERE user_id = ?",
                    hashed, row.get("user_id")
            );
        }
    }
}
