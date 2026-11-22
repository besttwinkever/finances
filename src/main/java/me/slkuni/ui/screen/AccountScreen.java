package me.slkuni.ui.screen;

import me.slkuni.service.FinanceService;
import me.slkuni.ui.ConsoleIO;
import me.slkuni.ui.Screen;
import me.slkuni.ui.ScreenId;
import me.slkuni.validation.LoginValidator;

import java.io.IOException;
import java.util.Arrays;

public class AccountScreen implements Screen {
    private static final int REGISTER = 1;
    private static final int LOGIN = 2;

    private final ConsoleIO io;
    private final FinanceService service;

    public AccountScreen(ConsoleIO io, FinanceService service) {
        this.io = io;
        this.service = service;
    }

    @Override
    public ScreenId show() throws IOException {
        int choice = io.readMenu("Аккаунт", "Сохранить и выйти", "Создать аккаунт", "Войти");
        if (choice == ConsoleIO.BACK) {
            return ScreenId.EXIT;
        }
        String login = LoginValidator.normalize(io.readField("Логин:"));
        char[] password = io.readPassword();
        try {
            switch (choice) {
                case REGISTER:
                    service.register(login, password);
                    io.println("Аккаунт создан. Выберите \"Войти\".");
                    return ScreenId.ACCOUNT;
                case LOGIN:
                    service.login(login, password);
                    io.println("Вы вошли как " + service.getCurrentLogin() + ".");
                    ReportText.showWarnings(io, service.getWalletForCurrentUser(), null);
                    return ScreenId.WALLET;
                default:
                    throw new IllegalArgumentException("Нет такого пункта.");
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
