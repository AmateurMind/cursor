package com.example.expenses.repository;

import com.example.expenses.model.Account;
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
public class AccountRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Account> findAll() {
        return jdbcTemplate.query(
                "SELECT id, user_id AS userId, name, type, currency_code AS currencyCode FROM accounts ORDER BY id",
                new BeanPropertyRowMapper<>(Account.class)
        );
    }

    public Optional<Account> findById(Long id) {
        List<Account> list = jdbcTemplate.query(
                "SELECT id, user_id AS userId, name, type, currency_code AS currencyCode FROM accounts WHERE id = ?",
                new BeanPropertyRowMapper<>(Account.class),
                id
        );
        return list.stream().findFirst();
    }

    public List<Account> findByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id AS userId, name, type, currency_code AS currencyCode FROM accounts WHERE user_id = ? ORDER BY id",
                new BeanPropertyRowMapper<>(Account.class),
                userId
        );
    }

    public Long create(Account a) {
        String sql = "INSERT INTO accounts (user_id, name, type, currency_code) VALUES (?, ?, ?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a.getUserId());
            ps.setString(2, a.getName());
            ps.setString(3, a.getType());
            ps.setString(4, a.getCurrencyCode());
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }
}
