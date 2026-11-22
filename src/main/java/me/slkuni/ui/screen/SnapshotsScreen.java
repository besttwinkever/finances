package me.slkuni.ui.screen;

import me.slkuni.model.WalletSnapshot;
import me.slkuni.service.FinanceService;
import me.slkuni.storage.FileStorage;
import me.slkuni.ui.ConsoleIO;
import me.slkuni.ui.Screen;
import me.slkuni.ui.ScreenId;

import java.io.IOException;
import java.nio.file.Path;

public class SnapshotsScreen implements Screen {
    private static final int EXPORT = 1;
    private static final int IMPORT = 2;

    private final ConsoleIO io;
    private final FinanceService service;
    private final FileStorage files;

    public SnapshotsScreen(ConsoleIO io, FinanceService service, FileStorage files) {
        this.io = io;
        this.service = service;
        this.files = files;
    }

    @Override
    public ScreenId show() throws IOException {
        int choice = io.readMenu("Снимки кошелька", "Назад", "Выгрузить снимок", "Восстановить из снимка");
        if (choice == ConsoleIO.BACK) {
            return ScreenId.REPORTS;
        }
        Path file = io.readPath("Путь к снимку:");
        switch (choice) {
            case EXPORT:
                files.exportWallet(file, service.getCurrentLogin(), service.getWalletForCurrentUser().createSnapshot());
                io.println("Снимок кошелька записан.");
                break;
            case IMPORT:
                WalletSnapshot snapshot = files.importWallet(file, service.getCurrentLogin());
                service.replaceWallet(snapshot);
                io.println("Кошелёк загружен из снимка.");
                ReportText.showWarnings(io, service.getWalletForCurrentUser(), null);
                break;
        }
        return ScreenId.SNAPSHOTS;
    }
}
