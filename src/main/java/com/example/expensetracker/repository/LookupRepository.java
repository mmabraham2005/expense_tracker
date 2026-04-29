package com.example.expensetracker.repository;

import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public LookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> findAllUsers() {
        String sql = "SELECT user_id, name, email FROM Users ORDER BY name";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new User(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("email")
            )
        );
    }

    public List<Category> findAllCategories() {
        String sql = "SELECT category_id, category_name FROM Categories ORDER BY category_name";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new Category(
                rs.getInt("category_id"),
                rs.getString("category_name")
            )
        );
    }
}