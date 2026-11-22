package me.slkuni.model;

public class Account {
    private final String login;
    private final String passwordHash;

    public Account(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
