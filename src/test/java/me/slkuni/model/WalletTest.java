package me.slkuni.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private final Wallet wallet = new Wallet();

    @Test
    void countsIncomeAndExpensesExactly() {
        wallet.createCategory("Работа", CategoryType.INCOME);
        wallet.createCategory("Еда", CategoryType.EXPENSE);
        wallet.addTransaction(CategoryType.INCOME, " Работа ", Money.parse("0.30"), DATE);
        wallet.addTransaction(CategoryType.EXPENSE, "еда", Money.parse("0,10"), DATE);
        assertEquals(new BigDecimal("0.20"), wallet.getBalance());
        assertEquals(2, wallet.createSnapshot().getTransactions().size());
    }

    @Test
    void rejectsInvalidMoneyAndRequiresPositiveTransactions() {
        for (String invalid : List.of("", "-1", "1.001", "NaN", "1e2", "1000000000000000")) {
            assertThrows(IllegalArgumentException.class, () -> Money.parse(invalid), invalid);
        }
        wallet.createCategory("Еда", CategoryType.EXPENSE);
        assertThrows(IllegalArgumentException.class,
                () -> wallet.addTransaction(CategoryType.EXPENSE, "Еда", BigDecimal.ZERO, DATE));
        assertTrue(wallet.createSnapshot().getTransactions().isEmpty());
    }

    @Test
    void preventsWrongTypeAndDuplicateCategories() {
        wallet.createCategory("Еда", CategoryType.EXPENSE);
        assertThrows(IllegalArgumentException.class, () -> wallet.createCategory(" ЕДА ", CategoryType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> wallet.createCategory(" ", CategoryType.EXPENSE));
        assertThrows(IllegalArgumentException.class,
                () -> wallet.addTransaction(CategoryType.INCOME, "Еда", BigDecimal.ONE, DATE));
        assertThrows(IllegalArgumentException.class, () -> wallet.createCategory("Входящий перевод", CategoryType.INCOME));
    }

    @Test
    void renamesCategoryWithoutLosingOperationsOrBudget() {
        wallet.createCategory("Еда", CategoryType.EXPENSE);
        wallet.addTransaction(CategoryType.EXPENSE, "Еда", BigDecimal.ONE, DATE);
        wallet.setBudget("Еда", new BigDecimal("10"));
        wallet.renameCategory("Еда", "Продукты");

        assertEquals(new BigDecimal("10.00"), wallet.getCategory("Продукты").getBudget());
        assertEquals("Продукты", wallet.createSnapshot().getTransactions().getFirst().getCategory());
        assertEquals(new BigDecimal("-1.00"), wallet.getBalance());
        assertThrows(IllegalArgumentException.class, () -> wallet.getCategory("Еда"));
    }

    @Test
    void deletesOnlyCategoriesWithoutOperations() {
        wallet.createCategory("Еда", CategoryType.EXPENSE);
        wallet.createCategory("Такси", CategoryType.EXPENSE);
        wallet.addTransaction(CategoryType.EXPENSE, "Еда", BigDecimal.ONE, DATE);
        assertThrows(IllegalArgumentException.class, () -> wallet.deleteCategory("Еда"));
        wallet.deleteCategory("Такси");
        assertEquals(1, wallet.createSnapshot().getCategories().size());
    }

    @Test
    void setsChangesAndRemovesExpenseBudget() {
        wallet.createCategory("Зарплата", CategoryType.INCOME);
        wallet.createCategory("Еда", CategoryType.EXPENSE);
        assertThrows(IllegalArgumentException.class, () -> wallet.setBudget("Зарплата", BigDecimal.ONE));
        wallet.setBudget("Еда", BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), wallet.getCategory("Еда").getBudget());
        wallet.setBudget("Еда", BigDecimal.TEN);
        assertEquals(new BigDecimal("10.00"), wallet.getCategory("Еда").getBudget());
        wallet.setBudget("Еда", null);
        assertNull(wallet.getCategory("Еда").getBudget());
    }

    @Test
    void rejectsSnapshotWithOperationInMissingCategory() {
        Transaction orphan = new Transaction("missing", BigDecimal.ONE, DATE, null);
        assertThrows(IllegalArgumentException.class,
                () -> new Wallet(new WalletSnapshot(List.of(), List.of(orphan))));
    }
}
