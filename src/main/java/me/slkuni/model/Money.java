package me.slkuni.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private static final BigDecimal AMOUNT_LIMIT = new BigDecimal("1000000000000000");

    public static BigDecimal parse(String text) {
        if (!text.matches("[0-9]{1,15}([.,][0-9]{1,2})?")) {
            throw new IllegalArgumentException("Введите сумму цифрами, с точностью до двух знаков после запятой.");
        }
        return new BigDecimal(text.replace(',', '.'));
    }

    public static BigDecimal positive(BigDecimal amount) {
        BigDecimal result = nonNegative(amount);
        if (result.signum() == 0) {
            throw new IllegalArgumentException("Сумма операции должна быть больше нуля.");
        }
        return result;
    }

    public static BigDecimal nonNegative(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2
                || amount.compareTo(AMOUNT_LIMIT) >= 0) {
            throw new IllegalArgumentException("Сумма должна быть от 0 до 999999999999999,99.");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
