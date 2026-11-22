package me.slkuni;

import me.slkuni.config.Config;
import me.slkuni.config.parser.YamlParser;
import me.slkuni.service.FinanceService;
import me.slkuni.storage.FileStorage;
import me.slkuni.storage.parser.JsonFinanceParser;
import me.slkuni.ui.ConsoleApplication;
import me.slkuni.ui.ConsoleIO;

import java.io.IOException;

public class Main {
    private static final String CONFIG_FILE_NAME = "config.yaml";

    private static Config config;
    private static FileStorage storage;
    private static FinanceService service;
    private static ConsoleIO io;
    private static ConsoleApplication application;

    public static void main() {
        try {
            initConfig();
            try (FileStorage activeStorage = initStorage()) {
                initServices(activeStorage);
                initUI();
                application.run();
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void initConfig() throws IOException {
        config = new Config(CONFIG_FILE_NAME, new YamlParser());
    }

    private static FileStorage initStorage() throws IOException {
        return new FileStorage(config.getDataFile(), new JsonFinanceParser());
    }

    private static void initServices(FileStorage activeStorage) {
        storage = activeStorage;
        service = new FinanceService(storage);
    }

    private static void initUI() {
        io = ConsoleIO.system();
        application = new ConsoleApplication(service, storage, io);
    }
}
