package me.slkuni.storage;

import me.slkuni.model.Account;
import me.slkuni.model.WalletSnapshot;

public class SavedWallet {
    private final Account account;
    private final WalletSnapshot wallet;

    public SavedWallet(Account account, WalletSnapshot wallet) {
        this.account = account;
        this.wallet = wallet;
    }

    public Account getAccount() {
        return account;
    }

    public WalletSnapshot getWallet() {
        return wallet;
    }
}
