package com.example.expenses.model;

import jakarta.validation.constraints.NotBlank;

public class User {
    private Long id;
    private String email;

    @NotBlank
    private String name;

    // Stored hash (SHA-256 for demo only)
    private String passwordHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
