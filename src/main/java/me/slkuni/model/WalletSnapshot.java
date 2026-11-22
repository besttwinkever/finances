package me.slkuni.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class WalletSnapshot {
    private final List<Category> categories;
    private final List<Transaction> transactions;

    @JsonCreator
    public WalletSnapshot(@JsonProperty("categories") List<Category> categories,
                          @JsonProperty("transactions") List<Transaction> transactions) {
        if (categories == null || transactions == null) {
            throw new IllegalArgumentException("В снимке кошелька должны быть категории и операции.");
        }
        this.categories = List.copyOf(categories);
        this.transactions = List.copyOf(transactions);
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
