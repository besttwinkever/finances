package me.slkuni.ui.screen;

import me.slkuni.model.Category;
import me.slkuni.model.CategoryType;
import me.slkuni.model.Wallet;
import me.slkuni.service.FinanceService;
import me.slkuni.ui.ConsoleIO;
import me.slkuni.ui.Screen;
import me.slkuni.ui.ScreenId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CategoriesScreen implements Screen {
    private static final int CREATE_CATEGORY = 1;
    private static final int FIRST_CATEGORY = 2;
    private static final int INCOME = 1;

    private final ConsoleIO io;
    private final FinanceService service;
    private final CategoryScreen categoryScreen;

    public CategoriesScreen(ConsoleIO io, FinanceService service, CategoryScreen categoryScreen) {
        this.io = io;
        this.service = service;
        this.categoryScreen = categoryScreen;
    }

    @Override
    public ScreenId show() throws IOException {
        Wallet wallet = service.getWalletForCurrentUser();
        List<Category> categories = wallet.getCategories();
        List<String> options = new ArrayList<>();
        options.add("Создать категорию");
        for (Category category : categories) {
            String type = category.getType() == CategoryType.INCOME ? "доход" : "расход";
            options.add(category.getName() + " (" + type + ")");
        }

        int choice = io.readMenu("Мои категории", "Назад", options.toArray(String[]::new));
        if (choice == ConsoleIO.BACK) {
            return ScreenId.WALLET;
        }
        if (choice == CREATE_CATEGORY) {
            int type = io.readMenu("Тип новой категории", "Назад", "Доход", "Расход");
            if (type != ConsoleIO.BACK) {
                String name = io.readField("Название:");
                wallet.createCategory(name, type == INCOME ? CategoryType.INCOME : CategoryType.EXPENSE);
                io.println("Категория создана.");
            }
            return ScreenId.CATEGORIES;
        }
        categoryScreen.select(categories.get(choice - FIRST_CATEGORY).getName());
        return ScreenId.CATEGORY;
    }
}
