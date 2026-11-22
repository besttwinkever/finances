package me.slkuni.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class Category {
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 60;

    private final String name;
    private final CategoryType type;
    private final BigDecimal budget;

    @JsonCreator
    public Category(@JsonProperty("name") String name, @JsonProperty("type") CategoryType type,
                    @JsonProperty("budget") BigDecimal budget) {
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH
                || name.indexOf('|') >= 0 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Название категории должно содержать от " + MIN_NAME_LENGTH
                    + " до " + MAX_NAME_LENGTH + " символов. Символ | запрещён.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Укажите тип категории.");
        }
        if (budget != null && type != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Бюджет можно задать только для расходов.");
        }
        this.name = name.strip();
        this.type = type;
        this.budget = budget == null ? null : Money.nonNegative(budget);
    }

    public String getName() {
        return name;
    }

    public CategoryType getType() {
        return type;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public boolean usedForTransfers() {
        return name.equals(type.getTransferCategory());
    }
}
