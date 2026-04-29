package com.example.expensetracker.model;

import java.math.BigDecimal;

public record CategoryTotal(

        String categoryName,

        BigDecimal total

) {}