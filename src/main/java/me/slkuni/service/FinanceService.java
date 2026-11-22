package me.slkuni.service;

import me.slkuni.model.Account;
import me.slkuni.model.CategoryType;
import me.slkuni.model.Money;
import me.slkuni.model.Wallet;
import me.slkuni.model.WalletSnapshot;
import me.slkuni.model.Transaction;
import me.slkuni.validation.LoginValidator;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FinanceService {
    private static final String TRANSFER_HISTORY_ERROR =
            "Снимок изменяет историю переводов. Восстановление отменено.";

    private final WalletStorage storage;
    private final Map<String, Account> accounts;
    private final Map<String, Wallet> wallets = new LinkedHashMap<>();
    private String currentLogin;

    public FinanceService(WalletStorage storage) {
        this.storage = storage;
        accounts = new LinkedHashMap<>(storage.getAccounts());
    }

    public void register(String input, char[] password) {
        String login = LoginValidator.normalize(input);
        if (accounts.containsKey(login)) {
            throw new IllegalArgumentException("Такой логин уже занят.");
        }
        accounts.put(login, new Account(login, PasswordHash.md5(password)));
        wallets.put(login, new Wallet());
    }

    public void login(String input, char[] password) throws IOException {
        String login = LoginValidator.normalize(input);
        Account account = accounts.get(login);
        if (account == null || !account.getPasswordHash().equals(PasswordHash.md5(password))) {
            throw new IllegalArgumentException("Неверный логин или пароль.");
        }
        getWalletForLogin(login);
        currentLogin = login;
    }

    public void logout() {
        currentLogin = null;
    }

    public String getCurrentLogin() {
        return currentLogin;
    }

    public Wallet getWalletForCurrentUser() {
        if (currentLogin == null) {
            throw new IllegalStateException("Сначала войдите в аккаунт.");
        }
        return wallets.get(currentLogin);
    }

    public void transfer(String input, BigDecimal amount, LocalDate date) throws IOException {
        Wallet sender = getWalletForCurrentUser();
        String recipientLogin = checkRecipient(input);
        amount = Money.positive(amount);
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("На кошельке недостаточно средств.");
        }
        Wallet recipient = getWalletForLogin(recipientLogin);
        sender.addTransfer(CategoryType.EXPENSE, amount, date, recipientLogin);
        recipient.addTransfer(CategoryType.INCOME, amount, date, currentLogin);
    }

    public String checkRecipient(String input) {
        String recipientLogin = LoginValidator.normalize(input);
        if (recipientLogin.equals(currentLogin)) {
            throw new IllegalArgumentException("Укажите логин другого пользователя.");
        }
        if (!accounts.containsKey(recipientLogin)) {
            throw new IllegalArgumentException("Получатель \"" + recipientLogin + "\" не найден.");
        }
        return recipientLogin;
    }

    public void replaceWallet(WalletSnapshot snapshot) {
        Wallet current = getWalletForCurrentUser();
        Wallet restored = new Wallet(snapshot);
        List<Transaction> currentTransfers = current.getTransfers();
        List<Transaction> restoredTransfers = restored.getTransfers();
        if (currentTransfers.size() != restoredTransfers.size()) {
            throw new IllegalArgumentException(TRANSFER_HISTORY_ERROR);
        }
        for (int i = 0; i < currentTransfers.size(); i++) {
            Transaction original = currentTransfers.get(i);
            Transaction replacement = restoredTransfers.get(i);
            if (!original.getCategory().equals(replacement.getCategory())
                    || original.getAmount().compareTo(replacement.getAmount()) != 0
                    || !original.getDate().equals(replacement.getDate())
                    || !original.getCounterparty().equals(replacement.getCounterparty())) {
                throw new IllegalArgumentException(TRANSFER_HISTORY_ERROR);
            }
        }
        wallets.put(currentLogin, restored);
    }

    public void save() throws IOException {
        storage.save(accounts, wallets);
    }

    private Wallet getWalletForLogin(String login) throws IOException {
        Wallet wallet = wallets.get(login);
        if (wallet == null) {
            wallet = storage.loadWallet(login);
            wallets.put(login, wallet);
        }
        return wallet;
    }
}
