package com.example.expensetracker.controller;

import com.example.expensetracker.model.Budget;
import com.example.expensetracker.model.CategoryTotal;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.MonthlyTotal;
import com.example.expensetracker.repository.BudgetRepository;
import com.example.expensetracker.repository.ExpenseRepository;
import com.example.expensetracker.repository.LookupRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Controller
public class AppController {

    private final LookupRepository lookupRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public AppController(
            LookupRepository lookupRepository,
            ExpenseRepository expenseRepository,
            BudgetRepository budgetRepository
    ) {
        this.lookupRepository = lookupRepository;
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("users", lookupRepository.findAllUsers());
        model.addAttribute("categories", lookupRepository.findAllCategories());
        model.addAttribute("successMessage", null);
        return "index";
    }

    @PostMapping("/expenses/add")
    public String addExpense(
            @RequestParam Integer userId,
            @RequestParam Integer categoryId,
            @RequestParam String expenseName,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate date,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String notes,
            Model model
    ) {
        expenseRepository.addExpense(
                new Expense(null, userId, categoryId, expenseName, amount, date, paymentMethod, notes)
        );

        model.addAttribute("users", lookupRepository.findAllUsers());
        model.addAttribute("categories", lookupRepository.findAllCategories());
        model.addAttribute("successMessage", "Expense added successfully.");
        return "index";
    }

    @GetMapping("/expenses/view")
    public String viewExpenses(Model model) {
        model.addAttribute("expenses", expenseRepository.findAllExpenseViews());
        return "view-expenses";
    }

    @GetMapping("/expenses/edit")
    public String editExpensePage(@RequestParam(required = false) Integer id, Model model) {
        model.addAttribute("expenses", expenseRepository.findAllExpenseViews());
        model.addAttribute("users", lookupRepository.findAllUsers());
        model.addAttribute("categories", lookupRepository.findAllCategories());

        if (id != null) {
            model.addAttribute("expense", expenseRepository.findExpenseById(id));
        }

        return "edit-expense";
    }

    @PostMapping("/expenses/update")
    public String updateExpense(
            @RequestParam Integer expenseId,
            @RequestParam Integer userId,
            @RequestParam Integer categoryId,
            @RequestParam String expenseName,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate date,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String notes
    ) {
        expenseRepository.updateExpense(
                new Expense(expenseId, userId, categoryId, expenseName, amount, date, paymentMethod, notes)
        );
        return "redirect:/expenses/view";
    }

    @GetMapping("/expenses/delete")
    public String deleteExpensePage(Model model) {
        model.addAttribute("expenses", expenseRepository.findAllExpenseViews());
        return "delete-expense";
    }

    @PostMapping("/expenses/delete")
    public String deleteExpense(@RequestParam Integer expenseId) {
        expenseRepository.deleteExpense(expenseId);
        return "redirect:/expenses/delete";
    }

    @GetMapping("/expenses/search")
    public String searchExpensePage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model
    ) {
        model.addAttribute("categories", lookupRepository.findAllCategories());
        model.addAttribute("expenses", expenseRepository.searchExpenses(keyword, categoryId, fromDate, toDate));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        return "search-expense";
    }

    @GetMapping("/reports")
    public String reportsPage(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer categoryId,
            Model model
    ) {
        BigDecimal totalSpending = expenseRepository.getTotalSpending(fromDate, toDate, categoryId);
        Integer transactionCount = expenseRepository.getTransactionCount(fromDate, toDate, categoryId);
        List<CategoryTotal> categoryTotals = expenseRepository.getCategoryTotals(fromDate, toDate, categoryId);
        List<MonthlyTotal> monthlyTotals = expenseRepository.getMonthlyTotals(fromDate, toDate, categoryId);

        if (totalSpending == null) {
            totalSpending = BigDecimal.ZERO;
        }

        if (transactionCount == null) {
            transactionCount = 0;
        }

        BigDecimal averageExpense = BigDecimal.ZERO;
        if (transactionCount > 0) {
            averageExpense = totalSpending.divide(
                    BigDecimal.valueOf(transactionCount),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        String topCategory = "N/A";
        for (CategoryTotal ct : categoryTotals) {
            if (ct.total() != null && ct.total().compareTo(BigDecimal.ZERO) > 0) {
                topCategory = ct.categoryName();
                break;
            }
        }

        model.addAttribute("totalSpending", totalSpending);
        model.addAttribute("transactionCount", transactionCount);
        model.addAttribute("averageExpense", averageExpense);
        model.addAttribute("topCategory", topCategory);

        model.addAttribute("categoryTotals", categoryTotals);
        model.addAttribute("monthlyTotals", monthlyTotals);

        model.addAttribute("categories", lookupRepository.findAllCategories());
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("selectedCategoryId", categoryId);

        return "reports";
    }

    @GetMapping("/budgets")
    public String budgetPage(Model model) {
        model.addAttribute("users", lookupRepository.findAllUsers());
        model.addAttribute("categories", lookupRepository.findAllCategories());
        model.addAttribute("budgets", budgetRepository.findAllBudgetViews());
        return "budget";
    }

    @PostMapping("/budgets/add")
    public String addBudget(
            @RequestParam Integer userId,
            @RequestParam Integer categoryId,
            @RequestParam BigDecimal limitAmount,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        budgetRepository.addBudget(
                new Budget(null, userId, categoryId, limitAmount, startDate, endDate)
        );
        return "redirect:/budgets";
    }
}