package me.slkuni.service;

import me.slkuni.model.Category;
import me.slkuni.model.CategoryType;
import me.slkuni.model.Transaction;
import me.slkuni.model.Wallet;
import me.slkuni.model.WalletSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FinanceReport {
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expense = BigDecimal.ZERO;
    private int transactionCount;
    private final List<CategoryReport> rows;

    public FinanceReport(Wallet wallet, List<String> names, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Начало периода должно быть не позже конца.");
        }
        WalletSnapshot snapshot = wallet.createSnapshot();
        Map<String, CategoryReport> selected = new LinkedHashMap<>();
        if (names.isEmpty()) {
            for (Category category : snapshot.getCategories()) {
                selected.put(category.getName(), new CategoryReport(category));
            }
        } else {
            for (String name : names) {
                Category category = wallet.getCategory(name);
                selected.putIfAbsent(category.getName(), new CategoryReport(category));
            }
        }
        rows = List.copyOf(selected.values());

        for (Transaction transaction : snapshot.getTransactions()) {
            CategoryReport row = selected.get(transaction.getCategory());
            if (row == null) {
                continue;
            }
            BigDecimal amount = transaction.getAmount();
            boolean isExpense = row.getCategory().getType() == CategoryType.EXPENSE;
            if (isExpense) {
                row.addExpense(amount);
            }
            LocalDate date = transaction.getDate();
            if ((from != null && date.isBefore(from)) || (to != null && date.isAfter(to))) {
                continue;
            }
            row.addToTotal(amount);
            if (isExpense) {
                expense = expense.add(amount);
            } else {
                income = income.add(amount);
            }
            transactionCount++;
        }
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public BigDecimal getBalance() {
        return income.subtract(expense);
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public List<CategoryReport> getRows() {
        return rows;
    }
}
