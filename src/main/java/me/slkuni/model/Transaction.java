package me.slkuni.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.slkuni.validation.LoginValidator;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {
    private final String category;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String counterparty;

    @JsonCreator
    public Transaction(@JsonProperty("category") String category, @JsonProperty("amount") BigDecimal amount,
                       @JsonProperty("date") LocalDate date, @JsonProperty("counterparty") String counterparty) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Укажите категорию операции.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Укажите дату операции.");
        }
        if (counterparty != null && !LoginValidator.isValid(counterparty)) {
            throw new IllegalArgumentException("Некорректный логин участника перевода.");
        }
        this.category = category.strip();
        this.amount = Money.positive(amount);
        this.date = date;
        this.counterparty = counterparty;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCounterparty() {
        return counterparty;
    }
}
