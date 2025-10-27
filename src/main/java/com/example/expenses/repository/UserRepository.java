package com.example.expenses.repository;

import com.example.expenses.model.User;
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
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> findAll() {
        return jdbcTemplate.query(
                "SELECT id, email, name, password_hash AS passwordHash FROM users ORDER BY id",
                new BeanPropertyRowMapper<>(User.class)
        );
    }

    public Optional<User> findById(Long id) {
        List<User> list = jdbcTemplate.query(
                "SELECT id, email, name, password_hash AS passwordHash FROM users WHERE id = ?",
                new BeanPropertyRowMapper<>(User.class),
                id
        );
        return list.stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        List<User> list = jdbcTemplate.query(
                "SELECT id, email, name, password_hash AS passwordHash FROM users WHERE email = ?",
                new BeanPropertyRowMapper<>(User.class),
                email
        );
        return list.stream().findFirst();
    }

    public Long create(User u) {
        String sql = "INSERT INTO users (email, name) VALUES (?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getName());
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public Long createWithPassword(String email, String name, String passwordHash) {
        String sql = "INSERT INTO users (email, name, password_hash) VALUES (?, ?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, email);
            ps.setString(2, name);
            ps.setString(3, passwordHash);
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }
}
