package com.example.expenses.controller;

import com.example.expenses.model.Account;
import com.example.expenses.repository.AccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping("/accounts")
    public List<Account> getAll() {
        return accountRepository.findAll();
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/accounts")
    public List<Account> getByUser(@PathVariable Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @PostMapping("/accounts")
    public ResponseEntity<Account> create(@Valid @RequestBody Account account) {
        Long id = accountRepository.create(account);
        account.setId(id);
        return ResponseEntity.created(URI.create("/api/accounts/" + id)).body(account);
    }
}
