package me.slkuni.storage;

import me.slkuni.model.Account;
import me.slkuni.model.Wallet;
import me.slkuni.model.WalletSnapshot;
import me.slkuni.service.WalletStorage;
import me.slkuni.storage.parser.FinanceParser;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

public class FileStorage implements WalletStorage, AutoCloseable {
    private static final String LOCK_FILE = "state.lock";

    private final Path stateFile;
    private final FinanceParser parser;
    private final FileChannel lockChannel;
    private Map<String, SavedWallet> storedWallets;

    public FileStorage(Path stateFile, FinanceParser parser) throws IOException {
        this.stateFile = stateFile.toAbsolutePath().normalize();
        this.parser = parser;

        Path directory = this.stateFile.getParent();
        if (directory == null || LOCK_FILE.equalsIgnoreCase(this.stateFile.getFileName().toString())) {
            throw new IllegalArgumentException("Укажите корректный путь в storage.file.");
        }
        Files.createDirectories(directory);
        lockChannel = FileChannel.open(Path.of(directory.toString(), LOCK_FILE), CREATE, WRITE);
        try {
            if (lockChannel.tryLock() == null) {
                throw new IOException("Каталог данных уже открыт другим экземпляром.");
            }
            storedWallets = readState();
        } catch (OverlappingFileLockException e) {
            lockChannel.close();
            throw new IOException("Каталог данных уже открыт другим экземпляром.", e);
        } catch (IOException | RuntimeException e) {
            try {
                lockChannel.close();
            } catch (IOException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    @Override
    public Map<String, Account> getAccounts() {
        Map<String, Account> accounts = new LinkedHashMap<>();
        for (SavedWallet savedWallet : storedWallets.values()) {
            Account account = savedWallet.getAccount();
            accounts.put(account.getLogin(), account);
        }
        return accounts;
    }

    @Override
    public Wallet loadWallet(String login) throws IOException {
        SavedWallet savedWallet = storedWallets.get(login);
        if (savedWallet == null) {
            throw new IOException("Не найден кошелёк для \"" + login + "\".");
        }
        try {
            return new Wallet(savedWallet.getWallet());
        } catch (IllegalArgumentException e) {
            throw new IOException("Не удалось загрузить кошелёк \"" + login + "\". " + e.getMessage(), e);
        }
    }

    @Override
    public void save(Map<String, Account> accounts, Map<String, Wallet> wallets) throws IOException {
        Map<String, SavedWallet> updatedWallets = new LinkedHashMap<>();
        for (Account account : accounts.values()) {
            String login = account.getLogin();
            Wallet wallet = wallets.get(login);
            WalletSnapshot snapshot;
            if (wallet == null) {
                snapshot = storedWallets.get(login).getWallet();
            } else {
                snapshot = wallet.createSnapshot();
            }
            updatedWallets.put(login, new SavedWallet(account, snapshot));
        }

        byte[] data = parser.writeState(updatedWallets);
        Path temporary = Files.createTempFile(stateFile.getParent(), "state-", ".tmp");
        try {
            Files.write(temporary, data);
            Files.move(temporary, stateFile, ATOMIC_MOVE, REPLACE_EXISTING);
            storedWallets = updatedWallets;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public void exportWallet(Path file, String login, WalletSnapshot snapshot) throws IOException {
        writeNewFile(file, parser.writeWallet(login, snapshot));
    }

    public WalletSnapshot importWallet(Path file, String login) throws IOException {
        return parser.readWallet(Files.readAllBytes(file), login);
    }

    public void writeReport(Path file, String text) throws IOException {
        writeNewFile(file, text.getBytes(StandardCharsets.UTF_8));
    }

    private void writeNewFile(Path file, byte[] data) throws IOException {
        Path name = file.getFileName();
        if (name != null && stateFile.getFileName().toString().equalsIgnoreCase(name.toString())
                && Files.isSameFile(file.toAbsolutePath().getParent(), stateFile.getParent())) {
            throw new IOException("Укажите путь вне файла состояния приложения.");
        }
        Files.write(file, data, CREATE_NEW, WRITE);
    }

    @Override
    public void close() throws IOException {
        lockChannel.close();
    }

    private Map<String, SavedWallet> readState() throws IOException {
        if (Files.notExists(stateFile)) {
            return Map.of();
        }
        try {
            return parser.readState(Files.readAllBytes(stateFile));
        } catch (IOException | IllegalArgumentException e) {
            throw new IOException("Не удалось открыть " + stateFile + ". " + e.getMessage(), e);
        }
    }
}
