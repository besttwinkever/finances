package me.slkuni.service;

import me.slkuni.model.Category;

import java.math.BigDecimal;

public class CategoryReport {
    public static final int WARNING_PERCENT = 80;

    private final Category category;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal allTimeExpense = BigDecimal.ZERO;

    CategoryReport(Category category) {
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getAllTimeExpense() {
        return allTimeExpense;
    }

    public BigDecimal getRemainingBudget() {
        return category.getBudget().subtract(allTimeExpense);
    }

    public boolean isNearBudget() {
        BigDecimal warningAmount = category.getBudget().multiply(BigDecimal.valueOf(WARNING_PERCENT, 2));
        return allTimeExpense.compareTo(warningAmount) >= 0 && getRemainingBudget().signum() > 0;
    }

    void addToTotal(BigDecimal amount) {
        total = total.add(amount);
    }

    void addExpense(BigDecimal amount) {
        allTimeExpense = allTimeExpense.add(amount);
    }
}
