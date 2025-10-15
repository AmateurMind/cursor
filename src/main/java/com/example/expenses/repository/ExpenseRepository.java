package com.example.expenses.repository;

import com.example.expenses.model.Expense;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ExpenseRepository {
    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Expense> findAll() {
        return jdbcTemplate.query(
                "SELECT id, title, amount, category, category_id AS categoryId, user_id AS userId, account_id AS accountId, expense_date AS expenseDate, notes FROM expenses ORDER BY expense_date DESC",
                new BeanPropertyRowMapper<>(Expense.class)
        );
    }

    public Optional<Expense> findById(Long id) {
        List<Expense> list = jdbcTemplate.query(
                "SELECT id, title, amount, category, category_id AS categoryId, user_id AS userId, account_id AS accountId, expense_date AS expenseDate, notes FROM expenses WHERE id = ?",
                new BeanPropertyRowMapper<>(Expense.class),
                id
        );
        return list.stream().findFirst();
    }

    public Long create(Expense e) {
        // Ensure category exists in lookup (auto-creates AI-suggested categories)
        jdbcTemplate.update(
                "INSERT IGNORE INTO categories (name) VALUES (?)",
                e.getCategory()
        );
        Long categoryId = null;
        try {
            categoryId = jdbcTemplate.queryForObject(
                    "SELECT id FROM categories WHERE name = ?",
                    Long.class,
                    e.getCategory()
            );
        } catch (Exception ignore) {}

        Long userId = (e.getUserId() != null ? e.getUserId() : 1L);
        Long accountId = (e.getAccountId() != null ? e.getAccountId() : 1L);

        String sql = "INSERT INTO expenses (title, amount, category, category_id, expense_date, notes, user_id, account_id) VALUES (?,?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long finalCategoryId = categoryId;
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, e.getTitle());
            ps.setBigDecimal(2, e.getAmount());
            ps.setString(3, e.getCategory());
            ps.setObject(4, finalCategoryId, java.sql.Types.BIGINT);
            ps.setDate(5, Date.valueOf(e.getExpenseDate()));
            ps.setString(6, e.getNotes());
            ps.setLong(7, userId);
            ps.setLong(8, accountId);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public int update(Long id, Expense e) {
        // Ensure category exists and resolve id
        jdbcTemplate.update(
                "INSERT IGNORE INTO categories (name) VALUES (?)",
                e.getCategory()
        );
        Long categoryId = null;
        try {
            categoryId = jdbcTemplate.queryForObject(
                    "SELECT id FROM categories WHERE name = ?",
                    Long.class,
                    e.getCategory()
            );
        } catch (Exception ignore) {}

        Long userId = e.getUserId();
        Long accountId = e.getAccountId();

        return jdbcTemplate.update(
                "UPDATE expenses SET title=?, amount=?, category=?, category_id=?, expense_date=?, notes=?, user_id=COALESCE(?, user_id), account_id=COALESCE(?, account_id) WHERE id=?",
                e.getTitle(), e.getAmount(), e.getCategory(), categoryId, Date.valueOf(e.getExpenseDate()), e.getNotes(), userId, accountId, id
        );
    }

    public int delete(Long id) {
        return jdbcTemplate.update("DELETE FROM expenses WHERE id=?", id);
    }
}


