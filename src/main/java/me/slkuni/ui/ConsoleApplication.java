package me.slkuni.ui;

import me.slkuni.service.FinanceService;
import me.slkuni.storage.FileStorage;
import me.slkuni.ui.screen.AccountScreen;
import me.slkuni.ui.screen.CategoriesScreen;
import me.slkuni.ui.screen.CategoryScreen;
import me.slkuni.ui.screen.OperationsScreen;
import me.slkuni.ui.screen.ReportsScreen;
import me.slkuni.ui.screen.SnapshotsScreen;
import me.slkuni.ui.screen.WalletScreen;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class ConsoleApplication {
    private final FinanceService service;
    private final ConsoleIO io;
    private final Map<ScreenId, Screen> screens;

    public ConsoleApplication(FinanceService service, FileStorage files, ConsoleIO io) {
        this.service = service;
        this.io = io;
        CategoryScreen categoryScreen = new CategoryScreen(io, service);
        screens = Map.of(
                ScreenId.ACCOUNT, new AccountScreen(io, service),
                ScreenId.WALLET, new WalletScreen(io, service),
                ScreenId.OPERATIONS, new OperationsScreen(io, service),
                ScreenId.CATEGORIES, new CategoriesScreen(io, service, categoryScreen),
                ScreenId.CATEGORY, categoryScreen,
                ScreenId.REPORTS, new ReportsScreen(io, service, files),
                ScreenId.SNAPSHOTS, new SnapshotsScreen(io, service, files));
    }

    public void run() throws IOException {
        io.println("Личные финансы");
        ScreenId current = service.getCurrentLogin() == null ? ScreenId.ACCOUNT : ScreenId.WALLET;
        while (true) {
            try {
                ScreenId next = screens.get(current).show();
                if (next == ScreenId.EXIT) {
                    save();
                    return;
                }
                current = next;
            } catch (EOFException e) {
                break;
            } catch (CancellationException e) {
                io.println("Ввод отменён.");
            } catch (FileAlreadyExistsException e) {
                io.println("Ошибка: файл уже существует: " + e.getFile());
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                io.println("Ошибка: " + e.getMessage());
            }
        }
        save();
    }

    private void save() throws IOException {
        service.save();
        io.println("Данные сохранены.");
    }
}
