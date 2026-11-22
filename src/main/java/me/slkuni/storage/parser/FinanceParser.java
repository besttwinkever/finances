package me.slkuni.storage.parser;

import me.slkuni.model.WalletSnapshot;
import me.slkuni.storage.SavedWallet;

import java.io.IOException;
import java.util.Map;

public interface FinanceParser {
    Map<String, SavedWallet> readState(byte[] data) throws IOException;

    byte[] writeState(Map<String, SavedWallet> wallets) throws IOException;

    WalletSnapshot readWallet(byte[] data, String login) throws IOException;

    byte[] writeWallet(String login, WalletSnapshot snapshot) throws IOException;
}
