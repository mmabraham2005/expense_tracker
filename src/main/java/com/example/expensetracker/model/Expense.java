package com.example.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Expense(
    Integer expenseId,
    Integer userId,
    Integer categoryId,
    String expenseName,
    BigDecimal amount,
    LocalDate date,
    String paymentMethod,
    String notes
) {}