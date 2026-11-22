package me.slkuni.ui;

import me.slkuni.model.CategoryType;
import me.slkuni.model.Wallet;
import me.slkuni.service.FinanceService;
import me.slkuni.storage.FileStorage;
import me.slkuni.storage.SavedWallet;
import me.slkuni.storage.parser.JsonFinanceParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleApplicationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @TempDir
    Path temp;

    @Test
    void navigatesMenusAndCreatesMissingCategoriesDuringOperations() throws IOException {
        String output = run(
                "1", "alice", "password",
                "1", "bob", "password",
                "2", "alice", "password",
                "1",                         // Операции
                "1", "1", "Зарплата", "1000", "",
                "2", "1", "Еда", "80", "2026-11-15",
                "3", "bob", "50",
                "0", "2", "3", "2", "100",   // Бюджет категории \"Еда\"
                "0", "0", "3", "1",          // Сводка
                "2", "2026-11-15", "2026-11-15", "2", "1",
                "0", "4", "2", "bob", "password",
                "3", "1", "0", "0");
        assertFalse(output.contains("Ошибка:"), output);
        assertTrue(output.contains("Кошелёк\nАккаунт: alice"), output);
        assertTrue(output.contains("> "), output);
        assertTrue(output.contains("израсходовано не менее 80%"), output);
        assertTrue(output.contains("Общий доход: 1,000.00"), output);
        assertTrue(output.contains("Общие расходы: 130.00"), output);
        assertTrue(output.contains("Общие расходы: 80.00"), output);
        assertTrue(output.contains("Общий доход: 50.00"), output);
        assertTrue(output.contains("Данные сохранены"), output);
    }

    @Test
    void cancelsFormAndSavesOnEofDuringNextForm() throws IOException {
        String output = run(
                "1", "canceled", "",
                "1", "alice", "password",
                "2", "alice", "password",
                "1",
                "1", "1", "Черновик", "",
                "1", "1", "Незавершённая категория");
        assertTrue(output.contains("Ввод отменён."), output);
        assertTrue(output.contains("Данные сохранены"), output);
        try (FileStorage storage = openStorage()) {
            assertFalse(storage.getAccounts().containsKey("canceled"));
            Wallet wallet = storage.loadWallet("alice");
            assertTrue(wallet.getCategories().isEmpty());
            assertTrue(wallet.createSnapshot().getTransactions().isEmpty());
        }
    }

    @Test
    void managesSelectedCategoryAndKeepsItsOperations() throws IOException {
        seedWallet("alice", wallet -> {
            wallet.createCategory("Зарплата", CategoryType.INCOME);
            wallet.createCategory("Еда", CategoryType.EXPENSE);
            wallet.createCategory("Такси", CategoryType.EXPENSE);
            wallet.addTransaction(CategoryType.EXPENSE, "Еда", new BigDecimal("80"), DATE);
        });
        String output = run(
                "2", "alice", "password", "2", "3",
                "2", "0",                    // Установить нулевой бюджет
                "2", "5",
                "2", "",                     // Отменить изменение
                "2", "120",                  // Изменить бюджет
                "3",                         // Снять бюджет
                "1", "Продукты", "0",        // Переименовать и вернуться к списку
                "3", "3",                    // Удалить категорию \"Такси\"
                "0", "0");
        assertFalse(output.contains("Ошибка:"), output);
        assertTrue(output.contains("Бюджет: 0.00"), output);
        assertTrue(output.contains("Ввод отменён."), output);
        assertTrue(output.substring(output.indexOf("Ввод отменён.")).contains("Бюджет: 5.00"), output);
        assertTrue(output.contains("Осталось: 40.00"), output);
        assertTrue(output.contains("Лимит снят."), output);
        assertTrue(output.contains("Категория удалена."), output);
        try (FileStorage storage = openStorage()) {
            Wallet wallet = storage.loadWallet("alice");
            assertNull(wallet.getCategory("Продукты").getBudget());
            assertEquals("Продукты", wallet.createSnapshot().getTransactions().getFirst().getCategory());
            assertThrows(IllegalArgumentException.class, () -> wallet.getCategory("Такси"));
        }
    }

    @Test
    void matchesTotalsAndBudgetsFromTheExample() throws IOException {
        seedWallet("alice", wallet -> {
            wallet.createCategory("Зарплата", CategoryType.INCOME);
            wallet.createCategory("Бонус", CategoryType.INCOME);
            wallet.createCategory("Еда", CategoryType.EXPENSE);
            wallet.createCategory("Развлечения", CategoryType.EXPENSE);
            wallet.createCategory("Коммунальные услуги", CategoryType.EXPENSE);
            wallet.createCategory("Такси", CategoryType.EXPENSE);
            wallet.setBudget("Еда", new BigDecimal("4000"));
            wallet.setBudget("Развлечения", new BigDecimal("3000"));
            wallet.setBudget("Коммунальные услуги", new BigDecimal("2500"));
            wallet.addTransaction(CategoryType.INCOME, "Зарплата", new BigDecimal("20000"), DATE);
            wallet.addTransaction(CategoryType.INCOME, "Зарплата", new BigDecimal("40000"), DATE);
            wallet.addTransaction(CategoryType.INCOME, "Бонус", new BigDecimal("3000"), DATE);
            wallet.addTransaction(CategoryType.EXPENSE, "Еда", new BigDecimal("300"), DATE);
            wallet.addTransaction(CategoryType.EXPENSE, "Еда", new BigDecimal("500"), DATE);
            wallet.addTransaction(CategoryType.EXPENSE, "Развлечения", new BigDecimal("3000"), DATE);
            wallet.addTransaction(CategoryType.EXPENSE, "Коммунальные услуги", new BigDecimal("3000"), DATE);
            wallet.addTransaction(CategoryType.EXPENSE, "Такси", new BigDecimal("1500"), DATE);
        });
        String output = run("2", "alice", "password", "3", "1", "0", "0");
        assertTrue(output.contains("Общий доход: 63,000.00"), output);
        assertTrue(output.contains("Общие расходы: 8,300.00"), output);
        assertTrue(output.contains("Баланс выборки: 54,700.00"), output);
        assertTrue(output.matches("(?s).*Зарплата\\s+Доход\\s+60,000\\.00.*"), output);
        assertTrue(output.matches("(?s).*Коммунальные услуги\\s+2,500\\.00\\s+3,000\\.00\\s+-500\\.00.*"), output);
        assertTrue(output.matches("(?s).*Еда\\s+4,000\\.00\\s+800\\.00\\s+3,200\\.00.*"), output);
        assertTrue(output.matches("(?s).*Развлечения\\s+3,000\\.00\\s+3,000\\.00\\s+0\\.00.*"), output);
        assertFalse(output.contains("Ошибка:"), output);
    }

    @Test
    void reportsValidationBeforeAskingForTheNextField() throws IOException {
        seedWallet("alice", wallet -> {
            wallet.createCategory("Зарплата", CategoryType.INCOME);
            wallet.createCategory("Еда", CategoryType.EXPENSE);
            wallet.addTransaction(CategoryType.INCOME, "Зарплата", new BigDecimal("100"), DATE);
            wallet.setBudget("Еда", new BigDecimal("100"));
        });
        String output = run(
                "2", "alice", "wrong",
                "2", "alice", "password", "1",
                "2", "99",                   // Неверный номер категории
                "2", "1", "xyz",             // Сумма проверяется до даты
                "2", "1", "4", "2026-02-30",
                "2", "2", "Еда",             // Категория проверяется до суммы
                "3", "missing",              // Получатель проверяется до суммы
                "2", "1", "80", "",
                "2", "1", "20", "",
                "2", "1", "1", "",
                "0", "3",
                "2", "", "", "99",
                "2", "2000-01-01", "2000-01-31", "", "1",
                "0", "0");
        String[] expected = {
                "Неверный логин или пароль", "Выберите пункт от 0 до 2",
                "Введите сумму цифрами", "Некорректная дата", "Категория \"Еда\" уже существует",
                "Получатель \"missing\" не найден", "Нет категории с номером 99",
                "израсходовано не менее 80%", "исчерпан лимит", "превышен лимит", "баланс равен нулю",
                "расходы превысили доходы", "Операций по заданным условиям нет", "Данные сохранены"
        };
        for (String message : expected) {
            assertTrue(output.contains(message), output);
        }
    }

    @Test
    void reloadsBothSidesOfTransferAfterExitAndPreservesUnvisitedWallets() throws IOException {
        seedWallet("alice", wallet -> {
            wallet.createCategory("Зарплата", CategoryType.INCOME);
            wallet.addTransaction(CategoryType.INCOME, "Зарплата", new BigDecimal("999999999999999.99"), DATE);
        });
        seedWallet("bob", wallet -> {});
        seedWallet("carol", wallet -> {
            wallet.createCategory("Бонус", CategoryType.INCOME);
            wallet.addTransaction(CategoryType.INCOME, "Бонус", new BigDecimal("5"), DATE);
        });
        String transfer = run("2", "alice", "password", "1", "3", "bob", "70,11", "0", "0");
        assertTrue(transfer.contains("Перевод выполнен."), transfer);
        try (FileStorage files = openStorage()) {
            Wallet alice = files.loadWallet("alice");
            Wallet bob = files.loadWallet("bob");
            assertEquals(new BigDecimal("999999999999929.88"), alice.getBalance());
            assertEquals("bob", alice.createSnapshot().getTransactions().getLast().getCounterparty());
            assertEquals(new BigDecimal("70.11"), bob.getBalance());
            assertEquals("alice", bob.createSnapshot().getTransactions().getFirst().getCounterparty());
            assertEquals(new BigDecimal("5.00"), files.loadWallet("carol").getBalance());
        }
        assertFalse(Files.readString(Path.of(temp.toString(), "data/state.json")).contains("\"password\""));
    }

    @Test
    void exportsRestoresAndWritesTextReportsWithoutOverwritingFiles() throws IOException {
        seedSalary();
        Path snapshot = Path.of(temp.toString(), "my wallet.json");
        Path report = Path.of(temp.toString(), "report.txt");
        String output = run(
                "2", "alice", "password", "3", "3", "1", snapshot.toString(),
                "0", "0", "1", "1", "1", "45", "2026-11-16",
                "0", "3", "3", "2", snapshot.toString(), "0",
                "2", "2026-11-15", "2026-11-15", "1", "2", report.toString(),
                "2", "", "", "", "2", report.toString(),
                "0", "0");
        assertTrue(output.contains("Кошелёк загружен из снимка."), output);
        assertTrue(output.contains("файл уже существует"), output);
        assertTrue(Files.readString(report).contains("Общий доход: 55.00"));
        assertFalse(Files.readString(snapshot).contains("passwordHash"));
        String restored = run("2", "alice", "password", "3", "1");
        assertTrue(restored.contains("Общий доход: 55.00"), restored);
    }

    @Test
    void rejectsInvalidAndOtherUsersBackupsWithoutReplacingWallet() throws IOException {
        seedSalary();
        Path bad = Files.writeString(Path.of(temp.toString(), "bad.json"), """
                {"version":1,"login":"alice","wallet":{"categories":[],"transactions":[
                {"category":"Missing","amount":1,"date":"2026-11-15","counterparty":null}]}}
                """);
        Path other = Files.writeString(Path.of(temp.toString(), "other.json"), """
                {"version":1,"login":"bob","wallet":{"categories":[],"transactions":[]}}
                """);
        String result = run("2", "alice", "password", "3", "3",
                "2", bad.toString(), "2", other.toString(), "0", "1");
        assertTrue(result.contains("Категория \"Missing\" не найдена"), result);
        assertTrue(result.contains("Снимок принадлежит другому логину"), result);
        assertTrue(result.contains("Общий доход: 55.00"), result);
    }

    @Test
    void reservesStateFilePathBeforeFirstSave() throws IOException {
        String state = Path.of(temp.toString(), "data/state.json").toString();
        String result = run(
                "1", "alice", "password", "2", "alice", "password", "3",
                "2", "", "", "2", state,
                "3", "1", state, "0", "0", "0");
        assertTrue(result.contains("Укажите путь вне файла состояния приложения"), result);
        assertFalse(result.contains("Отчёт записан"), result);
        assertFalse(result.contains("Снимок кошелька записан"), result);
        try (FileStorage storage = openStorage()) {
            assertTrue(storage.getAccounts().containsKey("alice"));
        }
    }

    @Test
    void rejectsInvalidPasswordHashWithoutChangingState() throws IOException {
        Path directory = Files.createDirectories(Path.of(temp.toString(), "data"));
        String state = """
                {"version":1,"users":{"alice":{
                  "account":{"login":"alice","passwordHash":"invalid"},
                  "wallet":{"categories":[],"transactions":[]}
                }}}
                """;
        Path stateFile = Files.writeString(Path.of(directory.toString(), "state.json"), state);
        IOException error = assertThrows(IOException.class, this::openStorage);
        assertTrue(error.getMessage().contains("ожидается хеш пароля MD5"));
        assertEquals(state, Files.readString(stateFile));
    }

    @Test
    void acceptsLiteralZeroInTextAndPasswordFields() throws IOException {
        try (FileStorage storage = openStorage()) {
            FinanceService service = new FinanceService(storage);
            service.register("0", "0".toCharArray());
            service.save();
        }
        String output = run("2", "0", "0", "2", "1", "2", "0", "0", "0");
        assertTrue(output.contains("Вы вошли как 0."), output);
        assertTrue(output.contains("Категория создана."), output);
        try (FileStorage storage = openStorage()) {
            assertEquals("0", storage.loadWallet("0").getCategory("0").getName());
        }
    }

    @Test
    void retriesExitAfterSaveFailure() throws IOException {
        JsonFinanceParser parser = new JsonFinanceParser() {
            private boolean failNextSave = true;

            @Override
            public byte[] writeState(Map<String, SavedWallet> wallets) throws IOException {
                if (failNextSave) {
                    failNextSave = false;
                    throw new IOException("Не удалось записать файл.");
                }
                return super.writeState(wallets);
            }
        };
        StringWriter text = new StringWriter();
        try (FileStorage storage = new FileStorage(Path.of(temp.toString(), "data/state.json"), parser)) {
            FinanceService service = new FinanceService(storage);
            StringReader input = new StringReader("1\nalice\npassword\n0\n0\n");
            ConsoleIO io = new ConsoleIO(new BufferedReader(input), new PrintWriter(text), null);
            new ConsoleApplication(service, storage, io).run();
        }
        String output = text.toString();
        assertTrue(output.contains("Не удалось записать файл."), output);
        assertTrue(output.contains("Данные сохранены."), output);
        try (FileStorage storage = openStorage()) {
            assertTrue(storage.getAccounts().containsKey("alice"));
        }
    }

    @Test
    void validatesLoginBeforeRequestingPassword() throws IOException {
        String output = run("1", "@@@", "2", "@@@", "0");
        assertTrue(output.contains("Логин должен содержать"), output);
        assertFalse(output.contains("Пароль"), output);
        try (FileStorage storage = openStorage()) {
            assertTrue(storage.getAccounts().isEmpty());
        }
    }

    @Test
    void preservesMultipleZerosInLoginAndPassword() throws IOException {
        String output = run("1", "000", "000", "2", "000", "000", "0");
        assertTrue(output.contains("Вы вошли как 000."), output);
        try (FileStorage storage = openStorage()) {
            FinanceService service = new FinanceService(storage);
            service.login("000", "000".toCharArray());
            assertEquals("000", service.getCurrentLogin());
            assertFalse(storage.getAccounts().containsKey("00"));
        }
    }

    @Test
    void blocksRestoreBeforeTransferThroughMenusAndAfterRestart() throws IOException {
        seedWallet("alice", wallet -> {
            wallet.createCategory("Доход", CategoryType.INCOME);
            wallet.addTransaction(CategoryType.INCOME, "Доход", BigDecimal.TEN, DATE);
        });
        seedWallet("bob", wallet -> {});
        Path backup = Path.of(temp.toString(), "before-transfer.json");
        String output = run("2", "alice", "password", "3", "3", "1", backup.toString(),
                "0", "0", "1", "3", "bob", "10", "0", "3", "3", "2", backup.toString(),
                "0", "0", "0");
        assertTrue(output.contains("Снимок изменяет историю переводов"), output);
        assertFalse(output.contains("Кошелёк загружен из снимка"), output);
        String restartedOutput = run("2", "alice", "password", "3", "3", "2", backup.toString(),
                "0", "0", "0");
        assertTrue(restartedOutput.contains("Снимок изменяет историю переводов"), restartedOutput);
        try (FileStorage storage = openStorage()) {
            assertEquals(new BigDecimal("0.00"), storage.loadWallet("alice").getBalance());
            assertEquals(new BigDecimal("10.00"), storage.loadWallet("bob").getBalance());
        }
    }

    @Test
    void rejectsInvalidMenuInputAndShowsOnlyAvailableChoices() throws IOException {
        String output = run("99", "register alice", "0");
        assertTrue(output.contains("1. Создать аккаунт"), output);
        assertTrue(output.contains("2. Войти"), output);
        assertEquals(2, output.split("Ошибка: Выберите пункт от 0 до 2\\.", -1).length - 1);
        assertFalse(output.contains("Справка"), output);
        assertTrue(output.contains("Данные сохранены"), output);
    }

    @Test
    void leavesCorruptStateIntact() throws IOException {
        Path directory = Files.createDirectories(Path.of(temp.toString(), "data"));
        Path state = Files.writeString(Path.of(directory.toString(), "state.json"), "{broken");
        assertThrows(IOException.class, this::openStorage);
        assertEquals("{broken", Files.readString(state));
        Files.delete(state);
        try (FileStorage storage = openStorage()) {
            assertTrue(storage.getAccounts().isEmpty());
        }
    }

    private void seedSalary() throws IOException {
        seedWallet("alice", wallet -> {
            wallet.createCategory("Зарплата", CategoryType.INCOME);
            wallet.addTransaction(CategoryType.INCOME, "Зарплата", new BigDecimal("55"), DATE);
        });
    }

    private void seedWallet(String login, Consumer<Wallet> setup) throws IOException {
        try (FileStorage storage = openStorage()) {
            FinanceService service = new FinanceService(storage);
            char[] password = "password".toCharArray();
            service.register(login, password);
            service.login(login, password);
            setup.accept(service.getWalletForCurrentUser());
            service.save();
        }
    }

    private String run(String... inputs) throws IOException {
        StringWriter text = new StringWriter();
        try (FileStorage storage = openStorage()) {
            FinanceService service = new FinanceService(storage);
            StringReader input = new StringReader(String.join("\n", inputs) + "\n");
            ConsoleIO io = new ConsoleIO(new BufferedReader(input), new PrintWriter(text), null);
            new ConsoleApplication(service, storage, io).run();
        }
        return text.toString().replace("\r\n", "\n");
    }

    private FileStorage openStorage() throws IOException {
        return new FileStorage(Path.of(temp.toString(), "data/state.json"), new JsonFinanceParser());
    }

}
