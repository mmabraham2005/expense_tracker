package com.example.expensetracker.model;

import java.math.BigDecimal;

public record MonthlyTotal(

        String monthLabel,

        BigDecimal total

) {}