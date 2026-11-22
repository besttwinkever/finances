package me.slkuni.ui.screen;

import me.slkuni.model.Category;
import me.slkuni.model.CategoryType;
import me.slkuni.model.Money;
import me.slkuni.model.Wallet;
import me.slkuni.service.FinanceService;
import me.slkuni.ui.ConsoleIO;
import me.slkuni.ui.Screen;
import me.slkuni.ui.ScreenId;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OperationsScreen implements Screen {
    private static final int ADD_INCOME = 1;
    private static final int ADD_EXPENSE = 2;
    private static final int TRANSFER = 3;

    private final ConsoleIO io;
    private final FinanceService service;

    public OperationsScreen(ConsoleIO io, FinanceService service) {
        this.io = io;
        this.service = service;
    }

    @Override
    public ScreenId show() throws IOException {
        int choice = io.readMenu("Операции", "Назад",
                "Добавить доход", "Добавить расход", "Перевести деньги другому пользователю");
        if (choice == ConsoleIO.BACK) {
            return ScreenId.WALLET;
        }
        switch (choice) {
            case ADD_INCOME:
                addTransaction(CategoryType.INCOME);
                break;
            case ADD_EXPENSE:
                addTransaction(CategoryType.EXPENSE);
                break;
            case TRANSFER:
                String login = service.checkRecipient(io.readField("Логин получателя:"));
                BigDecimal amount = Money.parse(io.readField("Сумма:"));
                service.transfer(login, amount, LocalDate.now());
                io.println("Перевод выполнен.");
                ReportText.showWarnings(io, service.getWalletForCurrentUser(), CategoryType.EXPENSE.getTransferCategory());
                break;
        }
        return ScreenId.OPERATIONS;
    }

    private void addTransaction(CategoryType type) throws IOException {
        Wallet wallet = service.getWalletForCurrentUser();
        List<Category> categories = new ArrayList<>();
        List<String> options = new ArrayList<>();
        for (Category category : wallet.getCategories()) {
            if (category.getType() == type && !category.usedForTransfers()) {
                categories.add(category);
                options.add(category.getName());
            }
        }
        options.add("Создать категорию");
        String title = type == CategoryType.INCOME ? "Категория дохода" : "Категория расхода";
        int choice = io.readMenu(title, "Назад", options.toArray(String[]::new));
        if (choice == ConsoleIO.BACK) {
            return;
        }

        boolean creating = choice == options.size();
        Category category = creating
                ? wallet.prepareCategory(io.readField("Название новой категории:"), type)
                : categories.get(choice - 1);
        BigDecimal amount = Money.positive(Money.parse(io.readField("Сумма:")));
        LocalDate date = io.readDate("Дата (Enter: сегодня):");
        if (date == null) {
            date = LocalDate.now();
        }
        if (creating) {
            wallet.addTransactionToNewCategory(category, amount, date);
            io.println("Категория создана.");
        } else {
            wallet.addTransaction(type, category.getName(), amount, date);
        }
        io.println("Операция добавлена.");
        ReportText.showWarnings(io, wallet, type == CategoryType.EXPENSE ? category.getName() : "");
    }
}
