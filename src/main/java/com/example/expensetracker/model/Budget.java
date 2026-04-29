package com.example.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Budget(
        Integer budgetId,
        Integer userId,
        Integer categoryId,
        BigDecimal limitAmount,
        LocalDate startDate,
        LocalDate endDate
) {}