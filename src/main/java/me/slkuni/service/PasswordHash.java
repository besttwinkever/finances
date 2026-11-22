package me.slkuni.service;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

class PasswordHash {
    private static final int MIN_PASSWORD_LENGTH = 1;
    private static final int MAX_PASSWORD_LENGTH = 128;

    static String md5(char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH
                || password.length > MAX_PASSWORD_LENGTH || isBlank(password)) {
            throw new IllegalArgumentException("Введите пароль от " + MIN_PASSWORD_LENGTH
                    + " до " + MAX_PASSWORD_LENGTH + " символов.");
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)));
            return HexFormat.of().formatHex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Не удалось вычислить MD5.", e);
        }
    }

    private static boolean isBlank(char[] password) {
        for (char character : password) {
            if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return true;
    }
}
