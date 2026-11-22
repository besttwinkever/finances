package me.slkuni.service;

import me.slkuni.model.CategoryType;
import me.slkuni.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceReportTest {
    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 11, 1);
    private static final LocalDate LAST_DAY = FIRST_DAY.plusDays(4);
    private final Wallet wallet = new Wallet();

    @BeforeEach
    void prepareWallet() {
        wallet.createCategory("Зарплата", CategoryType.INCOME);
        wallet.createCategory("Еда", CategoryType.EXPENSE);
        wallet.createCategory("Такси", CategoryType.EXPENSE);
        wallet.addTransaction(CategoryType.INCOME, "Зарплата", new BigDecimal("100"), FIRST_DAY);
        wallet.addTransaction(CategoryType.EXPENSE, "Еда", new BigDecimal("80"), LAST_DAY);
        wallet.addTransaction(CategoryType.EXPENSE, "Еда", new BigDecimal("5"), LAST_DAY.plusDays(1));
        wallet.addTransaction(CategoryType.EXPENSE, "Такси", new BigDecimal("10"), LAST_DAY);
        wallet.setBudget("Еда", new BigDecimal("100"));
    }

    @Test
    void filtersMultipleCategoriesAndInclusiveDatesWithoutCountingDuplicates() {
        FinanceReport report = new FinanceReport(wallet, List.of("Зарплата", "Еда", "еда"), FIRST_DAY, LAST_DAY);
        assertEquals(new BigDecimal("100.00"), report.getIncome());
        assertEquals(new BigDecimal("80.00"), report.getExpense());
        assertEquals(new BigDecimal("20.00"), report.getBalance());
        assertEquals(2, report.getTransactionCount());
        assertEquals(2, report.getRows().size());
    }

    @Test
    void measuresBudgetOverAllTimeEvenWithAnOperationDateFilter() {
        CategoryReport food = new FinanceReport(wallet, List.of("Еда"), FIRST_DAY, LAST_DAY).getRows().getFirst();
        assertEquals(new BigDecimal("15.00"), food.getRemainingBudget());
        assertTrue(food.isNearBudget());
        wallet.setBudget("Еда", new BigDecimal("80"));
        food = new FinanceReport(wallet, List.of("Еда"), null, null).getRows().getFirst();
        assertEquals(new BigDecimal("-5.00"), food.getRemainingBudget());
        assertFalse(food.isNearBudget());
    }

    @Test
    void reportsEmptyRangesAndRejectsUnknownCategoryOrReversedDates() {
        FinanceReport empty = new FinanceReport(wallet, List.of(), LAST_DAY.plusDays(2), null);
        assertEquals(0, empty.getTransactionCount());
        assertThrows(IllegalArgumentException.class,
                () -> new FinanceReport(wallet, List.of("Еда", "Одежда"), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new FinanceReport(wallet, List.of(), LAST_DAY, FIRST_DAY));
    }
}
