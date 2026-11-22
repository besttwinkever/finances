package me.slkuni.service;

import me.slkuni.model.Account;
import me.slkuni.model.Wallet;

import java.io.IOException;
import java.util.Map;

public interface WalletStorage {
    Map<String, Account> getAccounts();

    Wallet loadWallet(String login) throws IOException;

    void save(Map<String, Account> accounts, Map<String, Wallet> wallets) throws IOException;
}
