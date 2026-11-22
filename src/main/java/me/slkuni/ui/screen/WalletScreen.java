package me.slkuni.ui.screen;

import me.slkuni.service.FinanceService;
import me.slkuni.ui.ConsoleIO;
import me.slkuni.ui.Screen;
import me.slkuni.ui.ScreenId;

import java.io.IOException;

public class WalletScreen implements Screen {
    private static final int OPERATIONS = 1;
    private static final int CATEGORIES = 2;
    private static final int REPORTS = 3;
    private static final int LOGOUT = 4;

    private final ConsoleIO io;
    private final FinanceService service;

    public WalletScreen(ConsoleIO io, FinanceService service) {
        this.io = io;
        this.service = service;
    }

    @Override
    public ScreenId show() throws IOException {
        int choice = io.readMenu("Кошелёк\nАккаунт: " + service.getCurrentLogin(), "Сохранить и выйти",
                "Операции", "Мои категории", "Отчёты", "Выйти из аккаунта");
        switch (choice) {
            case OPERATIONS:
                return ScreenId.OPERATIONS;
            case CATEGORIES:
                return ScreenId.CATEGORIES;
            case REPORTS:
                return ScreenId.REPORTS;
            case LOGOUT:
                service.logout();
                io.println("Вы вышли из аккаунта.");
                return ScreenId.ACCOUNT;
            case ConsoleIO.BACK:
                return ScreenId.EXIT;
            default:
                throw new IllegalArgumentException("Нет такого пункта.");
        }
    }
}
