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
                "SELECT id, title, amount, category, expense_date AS expenseDate, notes FROM expenses ORDER BY expense_date DESC",
                new BeanPropertyRowMapper<>(Expense.class)
        );
    }

    public Optional<Expense> findById(Long id) {
        List<Expense> list = jdbcTemplate.query(
                "SELECT id, title, amount, category, expense_date AS expenseDate, notes FROM expenses WHERE id = ?",
                new BeanPropertyRowMapper<>(Expense.class),
                id
        );
        return list.stream().findFirst();
    }

    public Long create(Expense e) {
        String sql = "INSERT INTO expenses (title, amount, category, expense_date, notes) VALUES (?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, e.getTitle());
            ps.setBigDecimal(2, e.getAmount());
            ps.setString(3, e.getCategory());
            ps.setDate(4, Date.valueOf(e.getExpenseDate()));
            ps.setString(5, e.getNotes());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public int update(Long id, Expense e) {
        return jdbcTemplate.update(
                "UPDATE expenses SET title=?, amount=?, category=?, expense_date=?, notes=? WHERE id=?",
                e.getTitle(), e.getAmount(), e.getCategory(), Date.valueOf(e.getExpenseDate()), e.getNotes(), id
        );
    }

    public int delete(Long id) {
        return jdbcTemplate.update("DELETE FROM expenses WHERE id=?", id);
    }
}


