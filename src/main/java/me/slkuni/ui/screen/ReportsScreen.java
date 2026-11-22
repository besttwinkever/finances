package me.slkuni.ui.screen;

import me.slkuni.model.Category;
import me.slkuni.model.Wallet;
import me.slkuni.service.FinanceReport;
import me.slkuni.service.FinanceService;
import me.slkuni.storage.FileStorage;
import me.slkuni.ui.ConsoleIO;
import me.slkuni.ui.Screen;
import me.slkuni.ui.ScreenId;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportsScreen implements Screen {
    private static final int SUMMARY = 1;
    private static final int FILTERED_REPORT = 2;
    private static final int SNAPSHOTS = 3;
    private static final int OUTPUT_TERMINAL = 1;
    private static final int OUTPUT_FILE = 2;

    private final ConsoleIO io;
    private final FinanceService service;
    private final FileStorage files;

    public ReportsScreen(ConsoleIO io, FinanceService service, FileStorage files) {
        this.io = io;
        this.service = service;
        this.files = files;
    }

    @Override
    public ScreenId show() throws IOException {
        int choice = io.readMenu("Отчёты", "Назад",
                "Сводка за всё время", "Отчёт с фильтрами или выгрузкой", "Снимки кошелька");
        switch (choice) {
            case SUMMARY:
                io.print(ReportText.format(new FinanceReport(service.getWalletForCurrentUser(), List.of(), null, null)));
                return ScreenId.REPORTS;
            case FILTERED_REPORT:
                showFilteredReport();
                return ScreenId.REPORTS;
            case SNAPSHOTS:
                return ScreenId.SNAPSHOTS;
            case ConsoleIO.BACK:
                return ScreenId.WALLET;
            default:
                throw new IllegalArgumentException("Нет такого пункта.");
        }
    }

    private void showFilteredReport() throws IOException {
        LocalDate from = io.readDate("Начало периода (ГГГГ-ММ-ДД, Enter: без ограничения):");
        LocalDate to = io.readDate("Конец периода (ГГГГ-ММ-ДД, Enter: без ограничения):");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Начало периода должно быть не позже конца.");
        }
        Wallet wallet = service.getWalletForCurrentUser();
        FinanceReport report = new FinanceReport(wallet, readCategories(wallet.getCategories()), from, to);
        int output = io.readMenu("Куда вывести отчёт?", "Отмена", "В терминал", "В файл");
        if (output == OUTPUT_TERMINAL) {
            io.print(ReportText.format(report));
        } else if (output == OUTPUT_FILE) {
            Path file = io.readPath("Путь к отчёту:");
            files.writeReport(file, ReportText.format(report));
            io.println("Отчёт записан в " + file);
        }
    }

    private List<String> readCategories(List<Category> available) throws IOException {
        if (available.isEmpty()) {
            return List.of();
        }
        io.println("Категории");
        for (int i = 0; i < available.size(); i++) {
            io.println((i + 1) + ". " + available.get(i).getName());
        }
        String input = io.readOptionalField("Номера через пробел (Enter: все):");
        if (input.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String value : input.split("\\s+")) {
            int number;
            try {
                number = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\"" + value + "\" не является номером категории.");
            }
            if (number < 1 || number > available.size()) {
                throw new IllegalArgumentException("Нет категории с номером " + number + ".");
            }
            names.add(available.get(number - 1).getName());
        }
        return names;
    }
}
