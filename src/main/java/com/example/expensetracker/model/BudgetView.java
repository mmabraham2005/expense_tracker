package com.example.expensetracker.model;

import java.math.BigDecimal;

import java.time.LocalDate;

public record BudgetView(

        Integer budgetId,

        String userName,

        String categoryName,

        BigDecimal limitAmount,

        LocalDate startDate,

        LocalDate endDate,

        BigDecimal spentAmount,

        BigDecimal remainingAmount,

        String status

) {}