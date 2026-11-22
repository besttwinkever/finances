package me.slkuni.storage;

import me.slkuni.model.WalletSnapshot;
import me.slkuni.model.CategoryType;
import me.slkuni.storage.parser.JsonFinanceParser;
import me.slkuni.service.FinanceService;
import me.slkuni.storage.parser.FinanceParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageTest {
    @TempDir
    Path temp;

    @Test
    void retriesFailedWriteWithoutLosingWalletOrSavedState() throws IOException {
        Path stateFile = Path.of(temp.toString(), "state.json");
        Path previousFile = Path.of(temp.toString(), "previous.json");
        Path obstacle = Path.of(stateFile.toString(), "occupied");
        try (FileStorage storage = new FileStorage(stateFile, new JsonFinanceParser())) {
            FinanceService service = new FinanceService(storage);
            service.register("alice", "password".toCharArray());
            service.login("alice", "password".toCharArray());
            service.getWalletForCurrentUser().createCategory("Доход", CategoryType.INCOME);
            service.getWalletForCurrentUser().addTransaction(CategoryType.INCOME, "Доход", BigDecimal.TEN, LocalDate.now());
            service.save();
            String previousState = Files.readString(stateFile);

            Files.move(stateFile, previousFile);
            Files.createDirectory(stateFile);
            Files.writeString(obstacle, "занято");
            service.getWalletForCurrentUser().addTransaction(CategoryType.INCOME, "Доход", BigDecimal.ONE, LocalDate.now());
            assertThrows(IOException.class, service::save);
            assertEquals(previousState, Files.readString(previousFile));
            assertEquals(new BigDecimal("10.00"), storage.loadWallet("alice").getBalance());
            assertEquals(new BigDecimal("11.00"), service.getWalletForCurrentUser().getBalance());
            try (var files = Files.list(temp)) {
                assertTrue(files.noneMatch(path -> path.toString().endsWith(".tmp")));
            }

            Files.delete(obstacle);
            Files.delete(stateFile);
            Files.move(previousFile, stateFile);
            service.save();
        }
        try (FileStorage storage = new FileStorage(stateFile, new JsonFinanceParser())) {
            assertEquals(new BigDecimal("11.00"), storage.loadWallet("alice").getBalance());
        }
    }

    @Test
    void holdsLockUntilStorageCloses() throws IOException {
        Path stateFile = Path.of(temp.toString(), "state.json");
        try (FileStorage first = new FileStorage(stateFile, new JsonFinanceParser())) {
            assertThrows(IOException.class, () -> new FileStorage(stateFile, new JsonFinanceParser()));
            assertThrows(IOException.class, () -> new FileStorage(stateFile, new JsonFinanceParser()));
            assertTrue(first.getAccounts().isEmpty());
        }
        try (FileStorage reopened = new FileStorage(stateFile, new JsonFinanceParser())) {
            assertTrue(reopened.getAccounts().isEmpty());
        }
    }

    @Test
    void rejectsSecondApplicationProcess() throws Exception {
        Path stateFile = Path.of(temp.toString(), "state.json");
        Path config = Path.of(temp.toString(), "config.yaml");
        Files.writeString(config, "storage:\n  file: '" + stateFile.toString().replace('\\', '/') + "'\n");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        try (FileStorage first = new FileStorage(stateFile, new JsonFinanceParser())) {
            Process second = new ProcessBuilder(java, "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                    "-cp", System.getProperty("java.class.path"), "me.slkuni.Main")
                    .directory(temp.toFile())
                    .redirectErrorStream(true)
                    .start();
            try {
                second.getOutputStream().close();
                assertTrue(second.waitFor(10, TimeUnit.SECONDS), "Второй процесс не завершился.");
                String output = new String(second.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(1, second.exitValue(), output);
                assertTrue(output.contains("Каталог данных уже открыт другим экземпляром."), output);
                assertTrue(first.getAccounts().isEmpty());
            } finally {
                second.destroyForcibly();
            }
        }
    }

    @Test
    void usesInjectedParserForStateAndBackups() throws IOException {
        Path stateFile = Path.of(temp.toString(), "state.txt");
        Path backupFile = Path.of(temp.toString(), "backup.txt");
        TestParser parser = new TestParser();
        char[] password = "password".toCharArray();

        try (FileStorage storage = new FileStorage(stateFile, parser)) {
            FinanceService service = new FinanceService(storage);
            service.register("alice", password);
            service.login("alice", password);
            storage.exportWallet(backupFile, "alice", service.getWalletForCurrentUser().createSnapshot());
            service.save();
        }
        assertEquals("test-state", Files.readString(stateFile));
        assertEquals("test-backup:alice", Files.readString(backupFile));

        try (FileStorage storage = new FileStorage(stateFile, parser)) {
            FinanceService restarted = new FinanceService(storage);
            restarted.login("alice", password);
            assertEquals("alice", restarted.getCurrentLogin());
            assertTrue(storage.importWallet(backupFile, "alice").getTransactions().isEmpty());
        }
    }

    private static class TestParser implements FinanceParser {
        private Map<String, SavedWallet> wallets;
        private WalletSnapshot backup;

        @Override
        public Map<String, SavedWallet> readState(byte[] data) {
            assertEquals("test-state", new String(data, StandardCharsets.UTF_8));
            return wallets;
        }

        @Override
        public byte[] writeState(Map<String, SavedWallet> wallets) {
            this.wallets = wallets;
            return "test-state".getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public WalletSnapshot readWallet(byte[] data, String login) {
            assertEquals("test-backup:" + login, new String(data, StandardCharsets.UTF_8));
            return backup;
        }

        @Override
        public byte[] writeWallet(String login, WalletSnapshot snapshot) {
            backup = snapshot;
            return ("test-backup:" + login).getBytes(StandardCharsets.UTF_8);
        }
    }
}
