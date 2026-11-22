package me.slkuni.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Wallet {
    private final Map<String, Category> categories;
    private final List<Transaction> transactions;

    public Wallet() {
        categories = new LinkedHashMap<>();
        transactions = new ArrayList<>();
    }

    public Wallet(WalletSnapshot snapshot) {
        this();
        for (Category category : snapshot.getCategories()) {
            if (isTransferCategory(category.getName())
                    && !category.getName().equals(category.getType().getTransferCategory())) {
                throw new IllegalArgumentException("Название зарезервировано для переводов.");
            }
            addCategory(category);
        }
        for (Transaction transaction : snapshot.getTransactions()) {
            Category category = getCategory(transaction.getCategory());
            boolean transfer = transaction.getCounterparty() != null;
            if (transfer != isTransferCategory(category.getName())) {
                throw new IllegalArgumentException("Некорректная категория или логин участника перевода.");
            }
            transactions.add(new Transaction(category.getName(), transaction.getAmount(),
                    transaction.getDate(), transaction.getCounterparty()));
        }
    }

    public WalletSnapshot createSnapshot() {
        return new WalletSnapshot(getCategories(), transactions);
    }

    public List<Category> getCategories() {
        return List.copyOf(categories.values());
    }

    public List<Transaction> getTransfers() {
        List<Transaction> transfers = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getCounterparty() != null) {
                transfers.add(transaction);
            }
        }
        return transfers;
    }

    public Category getCategory(String name) {
        Category category = categories.get(categoryKey(name));
        if (category == null) {
            throw new IllegalArgumentException("Категория \"" + name + "\" не найдена.");
        }
        return category;
    }

    public void createCategory(String name, CategoryType type) {
        Category category = prepareCategory(name, type);
        categories.put(categoryKey(category.getName()), category);
    }

    public Category prepareCategory(String name, CategoryType type) {
        requireCustomCategory(name);
        Category category = new Category(name, type, null);
        if (categories.containsKey(categoryKey(category.getName()))) {
            throw new IllegalArgumentException("Категория \"" + category.getName() + "\" уже существует.");
        }
        return category;
    }

    public void renameCategory(String name, String newName) {
        requireCustomCategory(name);
        requireCustomCategory(newName);
        Category old = getCategory(name);
        Category renamed = new Category(newName, old.getType(), old.getBudget());
        String oldKey = categoryKey(old.getName());
        String newKey = categoryKey(renamed.getName());
        if (!oldKey.equals(newKey) && categories.containsKey(newKey)) {
            throw new IllegalArgumentException("Категория \"" + renamed.getName() + "\" уже существует.");
        }
        categories.remove(oldKey);
        categories.put(newKey, renamed);

        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            if (transaction.getCategory().equals(old.getName())) {
                transactions.set(i, new Transaction(renamed.getName(), transaction.getAmount(),
                        transaction.getDate(), null));
            }
        }
    }

    public void deleteCategory(String name) {
        requireCustomCategory(name);
        if (hasTransactions(name)) {
            throw new IllegalArgumentException("В категории есть операции. Её можно переименовать.");
        }
        categories.remove(categoryKey(name));
    }

    public boolean hasTransactions(String name) {
        String category = getCategory(name).getName();
        for (Transaction transaction : transactions) {
            if (transaction.getCategory().equals(category)) {
                return true;
            }
        }
        return false;
    }

    public void setBudget(String name, BigDecimal limit) {
        Category category = getCategory(name);
        if (category.getType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Бюджет можно задать только для расходов.");
        }
        categories.put(categoryKey(name), new Category(category.getName(), category.getType(), limit));
    }

    public void addTransaction(CategoryType type, String name, BigDecimal amount, LocalDate date) {
        requireCustomCategory(name);
        Category category = getCategory(name);
        if (category.getType() != type) {
            throw new IllegalArgumentException("Тип операции не совпадает с типом категории \"" + category.getName() + "\".");
        }
        transactions.add(new Transaction(category.getName(), amount, date, null));
    }

    public void addTransactionToNewCategory(Category category, BigDecimal amount, LocalDate date) {
        requireCustomCategory(category.getName());
        Transaction transaction = new Transaction(category.getName(), amount, date, null);
        addCategory(category);
        transactions.add(transaction);
    }

    public void addTransfer(CategoryType type, BigDecimal amount, LocalDate date, String counterparty) {
        if (counterparty == null) {
            throw new IllegalArgumentException("Укажите логин участника перевода.");
        }
        String name = type.getTransferCategory();
        Transaction transaction = new Transaction(name, amount, date, counterparty);
        categories.putIfAbsent(categoryKey(name), new Category(name, type, null));
        transactions.add(transaction);
    }

    public BigDecimal getBalance() {
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            CategoryType type = getCategory(transaction.getCategory()).getType();
            if (type == CategoryType.INCOME) {
                balance = balance.add(transaction.getAmount());
            } else {
                balance = balance.subtract(transaction.getAmount());
            }
        }
        return balance;
    }

    private void addCategory(Category category) {
        if (categories.putIfAbsent(categoryKey(category.getName()), category) != null) {
            throw new IllegalArgumentException("Категория \"" + category.getName() + "\" уже существует.");
        }
    }

    private String categoryKey(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Укажите название категории.");
        }
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private boolean isTransferCategory(String name) {
        return CategoryType.INCOME.getTransferCategory().equalsIgnoreCase(name.strip())
                || CategoryType.EXPENSE.getTransferCategory().equalsIgnoreCase(name.strip());
    }

    private void requireCustomCategory(String name) {
        if (name != null && isTransferCategory(name)) {
            throw new IllegalArgumentException("Эта категория используется для переводов.");
        }
    }
}
