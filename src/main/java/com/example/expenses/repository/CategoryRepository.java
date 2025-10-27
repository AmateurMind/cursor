package com.example.expenses.repository;

import com.example.expenses.model.Category;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findAll() {
        return jdbcTemplate.query(
                "SELECT id, name, type, color FROM categories ORDER BY name",
                new BeanPropertyRowMapper<>(Category.class)
        );
    }

    public Optional<Category> findById(Long id) {
        List<Category> list = jdbcTemplate.query(
                "SELECT id, name, type, color FROM categories WHERE id = ?",
                new BeanPropertyRowMapper<>(Category.class), id
        );
        return list.stream().findFirst();
    }

    public Long create(Category c) {
        String sql = "INSERT INTO categories (name, type, color) VALUES (?, COALESCE(?, 'expense'), ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, c.getName());
            ps.setString(2, c.getType());
            ps.setString(3, c.getColor());
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public int update(Long id, Category c) {
        return jdbcTemplate.update(
                "UPDATE categories SET name = ?, type = COALESCE(?, type), color = ? WHERE id = ?",
                c.getName(), c.getType(), c.getColor(), id
        );
    }

    public int delete(Long id) {
        return jdbcTemplate.update("DELETE FROM categories WHERE id = ?", id);
    }
}