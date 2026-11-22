package me.slkuni.ui;

import java.io.BufferedReader;
import java.io.Console;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CancellationException;

public class ConsoleIO {
    public static final int BACK = 0;

    private final BufferedReader input;
    private final PrintWriter output;
    private final Console console;

    public ConsoleIO(BufferedReader input, PrintWriter output, Console console) {
        this.input = input;
        this.output = output;
        this.console = console;
    }

    public static ConsoleIO system() {
        Console console = System.console();
        if (console != null && console.isTerminal()) {
            return new ConsoleIO(null, console.writer(), console);
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter output = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        return new ConsoleIO(input, output, null);
    }

    public int readMenu(String title, String backLabel, String... options) throws IOException {
        println("\n" + title);
        for (int i = 0; i < options.length; i++) {
            println((i + 1) + ". " + options[i]);
        }
        println("0. " + backLabel);

        String selection = readLine("> ").strip();
        try {
            int choice = Integer.parseInt(selection);
            if (choice >= 0 && choice <= options.length) {
                return choice;
            }
        } catch (NumberFormatException ignored) {
            // В обоих случаях показываем границы меню.
        }
        throw new IllegalArgumentException("Выберите пункт от 0 до " + options.length + ".");
    }

    public String readField(String prompt) throws IOException {
        String value = readOptionalField(prompt);
        if (value.isEmpty()) {
            throw new CancellationException();
        }
        return value;
    }

    public String readOptionalField(String prompt) throws IOException {
        return readLine(prompt + " ").strip();
    }

    public char[] readPassword() throws IOException {
        String prompt = "Пароль: ";
        char[] password = console == null ? readLine(prompt).toCharArray() : console.readPassword("%s", prompt);
        if (password == null) {
            throw new EOFException();
        }
        if (password.length == 0) {
            throw new CancellationException();
        }
        return password;
    }

    public LocalDate readDate(String prompt) throws IOException {
        String value = readOptionalField(prompt);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректная дата \"" + value + "\". Используйте формат ГГГГ-ММ-ДД.");
        }
    }

    public Path readPath(String prompt) throws IOException {
        return Path.of(readField(prompt));
    }

    public void print(String text) {
        output.print(text);
        output.flush();
    }

    public void println(String text) {
        output.println(text);
        output.flush();
    }

    private String readLine(String prompt) throws IOException {
        String line;
        if (console == null) {
            print(prompt);
            line = input.readLine();
        } else {
            line = console.readLine("%s", prompt);
        }
        if (line == null) {
            throw new EOFException();
        }
        return line;
    }
}
