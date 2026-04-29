package com.example.expensetracker.model;

public record User(
    Integer userId,
    String name,
    String email
) {}