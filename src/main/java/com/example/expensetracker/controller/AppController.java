package com.example.expensetracker.controller;

import com.example.expensetracker.model.Budget;
import com.example.expensetracker.model.BudgetView;
import com.example.expensetracker.model.CategoryTotal;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.ExpenseView;
import com.example.expensetracker.model.MonthlyTotal;
import com.example.expensetracker.model.User;
import com.example.expensetracker.repository.BudgetRepository;
import com.example.expensetracker.repository.ExpenseRepository;
import com.example.expensetracker.repository.LookupRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private String requireLogin(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        return null;
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("userRole"));
    }

    private Integer filterUserId(HttpSession session) {
        return isAdmin(session) ? null : (Integer) session.getAttribute("userId");
    }

    private void addSessionAttributes(Model model, HttpSession session) {
        model.addAttribute("sessionUserName", session.getAttribute("userName"));
        model.addAttribute("isAdmin", isAdmin(session));
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            Model model
    ) {
        User user = lookupRepository.findByEmailAndPassword(email, password);
        if (user == null) {
            model.addAttribute("error", "Invalid email or password.");
            return "login";
        }
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.userId());
        session.setAttribute("userName", user.name());
        session.setAttribute("userRole", user.role());
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/signup")
    public String signupPage(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/";
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            Model model
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            model.addAttribute("error", "All fields are required.");
            return "signup";
        }
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters.");
            return "signup";
        }
        if (lookupRepository.existsByEmail(email.trim().toLowerCase())) {
            model.addAttribute("error", "An account with that email already exists.");
            return "signup";
        }

        lookupRepository.registerUser(name, email, password);
        User newUser = lookupRepository.findByEmailAndPassword(email.trim().toLowerCase(), password);

        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("userId", newUser.userId());
        newSession.setAttribute("userName", newUser.name());
        newSession.setAttribute("userRole", newUser.role());
        return "redirect:/";
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        addSessionAttributes(model, session);
        model.addAttribute("categories", lookupRepository.findAllCategories());
        model.addAttribute("successMessage", null);
        return "index";
    }

    @PostMapping("/expenses/add")
    public String addExpense(
            @RequestParam Integer categoryId,
            @RequestParam String expenseName,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate date,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String notes,
            HttpSession session,
            Model model
    ) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        Integer userId = (Integer) session.getAttribute("userId");
        expenseRepository.addExpense(
                new Expense(null, userId, categoryId, expenseName, amount, date, paymentMethod, notes)
        );

        addSessionAttributes(model, session);
        model.addAttribute("categories", lookupRepository.findAllCategories());
        model.addAttribute("successMessage", "Expense added successfully.");
        return "index";
    }

    @GetMapping("/expenses/view")
    public String viewExpenses(HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        addSessionAttributes(model, session);
        model.addAttribute("expenses", expenseRepository.findAllExpenseViews(filterUserId(session)));
        return "view-expenses";
    }

    @GetMapping("/expenses/edit")
    public String editExpensePage(@RequestParam(required = false) Integer id, HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        addSessionAttributes(model, session);
        model.addAttribute("expenses", expenseRepository.findAllExpenseViews(filterUserId(session)));
        model.addAttribute("categories", lookupRepository.findAllCategories());

        if (id != null) {
            Expense expense = expenseRepository.findExpenseById(id);
            if (!isAdmin(session) && !expense.userId().equals(session.getAttribute("userId"))) {
                return "redirect:/expenses/edit";
            }
            model.addAttribute("expense", expense);
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
            @RequestParam(required = false) String notes,
            HttpSession session
    ) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        if (!isAdmin(session)) {
            Expense existing = expenseRepository.findExpenseById(expenseId);
            if (!existing.userId().equals(session.getAttribute("userId"))) {
                return "redirect:/expenses/edit";
            }
            userId = (Integer) session.getAttribute("userId");
        }

        expenseRepository.updateExpense(
                new Expense(expenseId, userId, categoryId, expenseName, amount, date, paymentMethod, notes)
        );
        return "redirect:/expenses/view";
    }

    @GetMapping("/expenses/delete")
    public String deleteExpensePage(HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        addSessionAttributes(model, session);
        model.addAttribute("expenses", expenseRepository.findAllExpenseViews(filterUserId(session)));
        return "delete-expense";
    }

    @PostMapping("/expenses/delete")
    public String deleteExpense(@RequestParam Integer expenseId, HttpSession session) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        if (!isAdmin(session)) {
            Expense expense = expenseRepository.findExpenseById(expenseId);
            if (!expense.userId().equals(session.getAttribute("userId"))) {
                return "redirect:/expenses/delete";
            }
        }

        expenseRepository.deleteExpense(expenseId);
        return "redirect:/expenses/delete";
    }

    @GetMapping("/expenses/search")
    public String searchExpensePage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            HttpSession session,
            Model model
    ) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        addSessionAttributes(model, session);
        model.addAttribute("categories", lookupRepository.findAllCategories());
        model.addAttribute("expenses", expenseRepository.searchExpenses(keyword, categoryId, fromDate, toDate, minAmount, maxAmount, filterUserId(session)));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);
        return "search-expense";
    }

    @GetMapping("/reports")
    public String reportsPage(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer categoryId,
            HttpSession session,
            Model model
    ) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        Integer filterUser = filterUserId(session);
        BigDecimal totalSpending = expenseRepository.getTotalSpending(fromDate, toDate, categoryId, filterUser);
        Integer transactionCount = expenseRepository.getTransactionCount(fromDate, toDate, categoryId, filterUser);
        List<CategoryTotal> categoryTotals = expenseRepository.getCategoryTotals(fromDate, toDate, categoryId, filterUser);
        List<MonthlyTotal> monthlyTotals = expenseRepository.getMonthlyTotals(fromDate, toDate, categoryId, filterUser);

        if (totalSpending == null) totalSpending = BigDecimal.ZERO;
        if (transactionCount == null) transactionCount = 0;

        BigDecimal averageExpense = BigDecimal.ZERO;
        if (transactionCount > 0) {
            averageExpense = totalSpending.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP);
        }

        String topCategory = "N/A";
        for (CategoryTotal ct : categoryTotals) {
            if (ct.total() != null && ct.total().compareTo(BigDecimal.ZERO) > 0) {
                topCategory = ct.categoryName();
                break;
            }
        }

        addSessionAttributes(model, session);
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
    public String budgetPage(HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        addSessionAttributes(model, session);
        model.addAttribute("categories", lookupRepository.findAllCategories());

        Integer filterUser = filterUserId(session);
        List<BudgetView> budgets = budgetRepository.findAllBudgetViews(filterUser);
        model.addAttribute("budgets", budgets);

        Map<Integer, List<ExpenseView>> budgetExpenses = new LinkedHashMap<>();
        for (BudgetView b : budgets) {
            budgetExpenses.put(b.budgetId(), budgetRepository.findExpensesForBudget(b.budgetId(), filterUser));
        }
        model.addAttribute("budgetExpenses", budgetExpenses);

        return "budget";
    }

    @PostMapping("/budgets/delete")
    public String deleteBudget(@RequestParam Integer budgetId, HttpSession session) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        Integer userId = isAdmin(session) ? null : (Integer) session.getAttribute("userId");
        budgetRepository.deleteBudget(budgetId, userId);
        return "redirect:/budgets";
    }

    @PostMapping("/budgets/add")
    public String addBudget(
            @RequestParam Integer categoryId,
            @RequestParam BigDecimal limitAmount,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            HttpSession session
    ) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        Integer userId = (Integer) session.getAttribute("userId");
        budgetRepository.addBudget(
                new Budget(null, userId, categoryId, limitAmount, startDate, endDate)
        );
        return "redirect:/budgets";
    }

    @GetMapping("/admin/users")
    public String adminUsersPage(HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;
        if (!isAdmin(session)) return "redirect:/";

        addSessionAttributes(model, session);
        model.addAttribute("users", lookupRepository.findAllUsers());
        return "admin-users";
    }
}
