package com.example.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseView(
    Integer expenseId,
    String userName,
    String categoryName,
    String expenseName,
    BigDecimal amount,
    LocalDate date,
    String paymentMethod,
    String notes
) {}