package me.slkuni.validation;

import java.util.Locale;

public class LoginValidator {
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 32;
    private static final String PATTERN = "[a-z0-9_-]{" + MIN_LENGTH + "," + MAX_LENGTH + "}";

    public static boolean isValid(String login) {
        return login != null && login.matches(PATTERN);
    }

    public static String normalize(String input) {
        String login = input == null ? "" : input.strip().toLowerCase(Locale.ROOT);
        if (!isValid(login)) {
            throw new IllegalArgumentException("Логин должен содержать от " + MIN_LENGTH + " до " + MAX_LENGTH
                    + " латинских букв, цифр, дефисов или подчёркиваний.");
        }
        return login;
    }
}
