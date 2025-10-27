package com.example.expenses.model;

import jakarta.validation.constraints.NotBlank;

public class Category {
    private Long id;

    @NotBlank
    private String name;

    private String type; // e.g., expense

    private String color; // optional hex color

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}