package me.slkuni.service;

import me.slkuni.model.Account;
import me.slkuni.model.Category;
import me.slkuni.model.CategoryType;
import me.slkuni.model.Wallet;
import me.slkuni.model.WalletSnapshot;
import me.slkuni.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final char[] PASSWORD = "password".toCharArray();
    private final MemoryStorage storage = new MemoryStorage();
    private final FinanceService service = new FinanceService(storage);

    @BeforeEach
    void registerUsers() {
        service.register("Alice", PASSWORD);
        service.register("bob", PASSWORD);
    }

    @Test
    void authenticatesAndKeepsWalletsSeparate() throws IOException {
        assertThrows(IllegalStateException.class, service::getWalletForCurrentUser);
        assertThrows(IllegalArgumentException.class, () -> service.login("alice", "wrong".toCharArray()));
        assertNull(service.getCurrentLogin());
        assertThrows(IllegalArgumentException.class, () -> service.register("ALICE", PASSWORD));
        service.login("alice", PASSWORD);
        service.getWalletForCurrentUser().createCategory("Работа", CategoryType.INCOME);
        service.logout();
        assertThrows(IllegalStateException.class, service::getWalletForCurrentUser);
        service.login("bob", PASSWORD);
        assertTrue(service.getWalletForCurrentUser().createSnapshot().getCategories().isEmpty());
    }

    @Test
    void storesMd5PasswordHash() throws IOException {
        service.save();
        assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", storage.accounts.get("alice").getPasswordHash());
    }

    @Test
    void recordsBothSidesOfTransferWithLogins() throws IOException {
        fundAlice();
        service.transfer("BOB", new BigDecimal("5"), DATE);
        assertEquals(new BigDecimal("5.00"), service.getWalletForCurrentUser().getBalance());
        assertEquals("bob", service.getWalletForCurrentUser().createSnapshot().getTransactions().getLast().getCounterparty());

        service.login("bob", PASSWORD);
        assertEquals(new BigDecimal("5.00"), service.getWalletForCurrentUser().getBalance());
        assertEquals("alice", service.getWalletForCurrentUser().createSnapshot().getTransactions().getFirst().getCounterparty());
    }

    @Test
    void leavesBothWalletsUnchangedOnRejectedTransfers() throws IOException {
        fundAlice();
        assertThrows(IllegalArgumentException.class, () -> service.transfer("bob", new BigDecimal("11"), DATE));
        assertThrows(IllegalArgumentException.class, () -> service.transfer("alice", BigDecimal.ONE, DATE));
        assertThrows(IllegalArgumentException.class, () -> service.transfer("missing", BigDecimal.ONE, DATE));
        assertEquals(new BigDecimal("10.00"), service.getWalletForCurrentUser().getBalance());
        assertEquals(1, service.getWalletForCurrentUser().createSnapshot().getTransactions().size());
        service.login("bob", PASSWORD);
        assertTrue(service.getWalletForCurrentUser().createSnapshot().getTransactions().isEmpty());
    }

    @Test
    void rejectsRestoreBeforeOutgoingTransfer() throws IOException {
        fundAlice();
        WalletSnapshot beforeTransfer = service.getWalletForCurrentUser().createSnapshot();
        service.transfer("bob", BigDecimal.TEN, DATE);
        assertThrows(IllegalArgumentException.class, () -> service.replaceWallet(beforeTransfer));
        assertEquals(new BigDecimal("0.00"), service.getWalletForCurrentUser().getBalance());
        service.login("bob", PASSWORD);
        assertEquals(new BigDecimal("10.00"), service.getWalletForCurrentUser().getBalance());
    }

    @Test
    void rejectsRestoreBeforeIncomingTransfer() throws IOException {
        fundAlice();
        service.login("bob", PASSWORD);
        WalletSnapshot beforeTransfer = service.getWalletForCurrentUser().createSnapshot();
        service.login("alice", PASSWORD);
        service.transfer("bob", BigDecimal.TEN, DATE);
        service.login("bob", PASSWORD);
        assertThrows(IllegalArgumentException.class, () -> service.replaceWallet(beforeTransfer));
        assertEquals(new BigDecimal("10.00"), service.getWalletForCurrentUser().getBalance());
    }

    @Test
    void rejectsChangesToTransferFieldsAndOrder() throws IOException {
        fundAlice();
        service.transfer("bob", BigDecimal.ONE, DATE);
        service.transfer("bob", BigDecimal.ONE, DATE.plusDays(1));
        WalletSnapshot original = service.getWalletForCurrentUser().createSnapshot();
        Transaction first = original.getTransactions().get(1);
        Transaction second = original.getTransactions().get(2);
        List<Transaction> replacements = List.of(
                new Transaction(first.getCategory(), BigDecimal.TEN, first.getDate(), first.getCounterparty()),
                new Transaction(first.getCategory(), first.getAmount(), DATE.plusDays(2), first.getCounterparty()),
                new Transaction(first.getCategory(), first.getAmount(), first.getDate(), "alice"),
                new Transaction(CategoryType.INCOME.getTransferCategory(), first.getAmount(), first.getDate(), "bob"));
        for (Transaction replacement : replacements) {
            List<Transaction> transactions = new ArrayList<>(original.getTransactions());
            transactions.set(1, replacement);
            List<Category> categories = new ArrayList<>(original.getCategories());
            if (replacement.getCategory().equals(CategoryType.INCOME.getTransferCategory())) {
                categories.add(new Category(replacement.getCategory(), CategoryType.INCOME, null));
            }
            WalletSnapshot changed = new WalletSnapshot(categories, transactions);
            assertThrows(IllegalArgumentException.class, () -> service.replaceWallet(changed));
        }
        List<Transaction> reordered = List.of(original.getTransactions().getFirst(), second, first);
        assertThrows(IllegalArgumentException.class,
                () -> service.replaceWallet(new WalletSnapshot(original.getCategories(), reordered)));
        assertEquals(new BigDecimal("8.00"), service.getWalletForCurrentUser().getBalance());
    }

    @Test
    void restoresOrdinaryOperationsWhenTransfersAreUnchanged() throws IOException {
        fundAlice();
        service.transfer("bob", BigDecimal.ONE, DATE);
        WalletSnapshot afterTransfer = service.getWalletForCurrentUser().createSnapshot();
        service.getWalletForCurrentUser().addTransaction(CategoryType.INCOME, "Работа", BigDecimal.TEN, DATE);

        // Восстановленный снимок содержит новые экземпляры операций после разбора файла.
        List<Transaction> transactions = new ArrayList<>();
        for (Transaction transaction : afterTransfer.getTransactions()) {
            transactions.add(new Transaction(transaction.getCategory(), transaction.getAmount(),
                    transaction.getDate(), transaction.getCounterparty()));
        }
        service.replaceWallet(new WalletSnapshot(afterTransfer.getCategories(), transactions));
        assertEquals(new BigDecimal("9.00"), service.getWalletForCurrentUser().getBalance());
        service.login("bob", PASSWORD);
        assertEquals(new BigDecimal("1.00"), service.getWalletForCurrentUser().getBalance());
    }

    @Test
    void rejectsAddingTransfersThroughImport() throws IOException {
        fundAlice();
        Wallet wallet = new Wallet(service.getWalletForCurrentUser().createSnapshot());
        wallet.addTransfer(CategoryType.EXPENSE, BigDecimal.ONE, DATE, "bob");
        assertThrows(IllegalArgumentException.class, () -> service.replaceWallet(wallet.createSnapshot()));
        assertEquals(new BigDecimal("10.00"), service.getWalletForCurrentUser().getBalance());
    }

    @Test
    void doesNotDebitSenderWhenRecipientCannotBeLoaded() throws IOException {
        fundAlice();
        service.save();
        FinanceService restarted = new FinanceService(storage);
        restarted.login("alice", PASSWORD);
        storage.wallets.remove("bob");
        assertThrows(IOException.class, () -> restarted.transfer("bob", BigDecimal.ONE, DATE));
        assertEquals(new BigDecimal("10.00"), restarted.getWalletForCurrentUser().getBalance());
    }

    private void fundAlice() throws IOException {
        service.login("Alice", PASSWORD);
        service.getWalletForCurrentUser().createCategory("Работа", CategoryType.INCOME);
        service.getWalletForCurrentUser().addTransaction(CategoryType.INCOME, "Работа", BigDecimal.TEN, DATE);
    }

    private static class MemoryStorage implements WalletStorage {
        private Map<String, Account> accounts = Map.of();
        private final Map<String, WalletSnapshot> wallets = new HashMap<>();

        @Override
        public Map<String, Account> getAccounts() {
            return accounts;
        }

        @Override
        public Wallet loadWallet(String login) throws IOException {
            WalletSnapshot snapshot = wallets.get(login);
            if (snapshot == null) {
                throw new IOException("Кошелёк отсутствует.");
            }
            return new Wallet(snapshot);
        }

        @Override
        public void save(Map<String, Account> accounts, Map<String, Wallet> wallets) {
            this.accounts = Map.copyOf(accounts);
            wallets.forEach((login, wallet) -> this.wallets.put(login, wallet.createSnapshot()));
        }
    }
}
