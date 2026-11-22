package me.slkuni.ui.screen;

import me.slkuni.model.Category;
import me.slkuni.model.CategoryType;
import me.slkuni.model.Money;
import me.slkuni.model.Wallet;
import me.slkuni.service.FinanceReport;
import me.slkuni.service.FinanceService;
import me.slkuni.ui.ConsoleIO;
import me.slkuni.ui.Screen;
import me.slkuni.ui.ScreenId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CategoryScreen implements Screen {
    private enum Action {
        RENAME("Переименовать"),
        SET_BUDGET("Установить бюджет"),
        CHANGE_BUDGET("Изменить бюджет"),
        REMOVE_BUDGET("Снять бюджет"),
        DELETE("Удалить");

        private final String label;

        Action(String label) {
            this.label = label;
        }
    }

    private final ConsoleIO io;
    private final FinanceService service;
    private String selectedName;

    public CategoryScreen(ConsoleIO io, FinanceService service) {
        this.io = io;
        this.service = service;
    }

    void select(String name) {
        selectedName = name;
    }

    @Override
    public ScreenId show() throws IOException {
        Wallet wallet = service.getWalletForCurrentUser();
        Category category = wallet.getCategory(selectedName);
        List<Action> actions = new ArrayList<>();
        String title = "Категория: " + category.getName();
        if (!category.usedForTransfers()) {
            actions.add(Action.RENAME);
        }
        if (category.getType() == CategoryType.EXPENSE) {
            if (category.getBudget() == null) {
                actions.add(Action.SET_BUDGET);
            } else {
                actions.add(Action.CHANGE_BUDGET);
                actions.add(Action.REMOVE_BUDGET);
                FinanceReport report = new FinanceReport(wallet, List.of(selectedName), null, null);
                title += "\nБюджет: " + ReportText.money(category.getBudget())
                        + "\nОсталось: " + ReportText.money(report.getRows().getFirst().getRemainingBudget());
            }
        }
        if (!category.usedForTransfers() && !wallet.hasTransactions(selectedName)) {
            actions.add(Action.DELETE);
        }

        String[] labels = new String[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            labels[i] = actions.get(i).label;
        }
        int choice = io.readMenu(title, "Назад", labels);
        if (choice == ConsoleIO.BACK) {
            return ScreenId.CATEGORIES;
        }

        switch (actions.get(choice - 1)) {
            case RENAME:
                String name = io.readField("Новое название:");
                wallet.renameCategory(selectedName, name);
                selectedName = name;
                io.println("Категория переименована.");
                break;
            case SET_BUDGET:
            case CHANGE_BUDGET:
                String limit = io.readField("Лимит:");
                wallet.setBudget(selectedName, Money.parse(limit));
                io.println("Бюджет сохранён.");
                ReportText.showWarnings(io, wallet, selectedName);
                break;
            case REMOVE_BUDGET:
                wallet.setBudget(selectedName, null);
                io.println("Лимит снят.");
                break;
            case DELETE:
                wallet.deleteCategory(selectedName);
                io.println("Категория удалена.");
                return ScreenId.CATEGORIES;
        }
        return ScreenId.CATEGORY;
    }
}
