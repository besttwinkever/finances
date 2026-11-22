package me.slkuni.ui.screen;

import me.slkuni.model.Category;
import me.slkuni.model.CategoryType;
import me.slkuni.model.Wallet;
import me.slkuni.service.CategoryReport;
import me.slkuni.service.FinanceReport;
import me.slkuni.ui.ConsoleIO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

class ReportText {
    static String format(FinanceReport report) {
        try (Formatter text = new Formatter(Locale.US)) {
            text.format("Общий доход: %,.2f%nОбщие расходы: %,.2f%nБаланс выборки: %,.2f%n",
                    report.getIncome(), report.getExpense(), report.getBalance());
            if (report.getTransactionCount() == 0) {
                text.format("Операций по заданным условиям нет.%n");
            }
            int nameWidth = 12;
            List<CategoryReport> budgets = new ArrayList<>();
            for (CategoryReport row : report.getRows()) {
                nameWidth = Math.max(nameWidth, row.getCategory().getName().length());
                if (row.getCategory().getBudget() != null) {
                    budgets.add(row);
                }
            }
            if (!report.getRows().isEmpty()) {
                String rowFormat = "%-" + nameWidth + "s  %-7s  %20s%n";
                text.format("%n");
                text.format(rowFormat, "Категория", "Тип", "Сумма");
                for (CategoryReport row : report.getRows()) {
                    String type = row.getCategory().getType() == CategoryType.INCOME ? "Доход" : "Расход";
                    text.format(rowFormat, row.getCategory().getName(), type, money(row.getTotal()));
                }
            }
            if (!budgets.isEmpty()) {
                String rowFormat = "%-" + nameWidth + "s  %20s  %20s  %20s%n";
                text.format("%nБюджеты за всё время%n");
                text.format(rowFormat, "Категория", "Лимит", "Потрачено", "Осталось");
                for (CategoryReport row : budgets) {
                    text.format(rowFormat, row.getCategory().getName(), money(row.getCategory().getBudget()),
                            money(row.getAllTimeExpense()), money(row.getRemainingBudget()));
                }
            }
            return text.toString();
        }
    }

    static void showWarnings(ConsoleIO io, Wallet wallet, String categoryName) {
        FinanceReport report = new FinanceReport(wallet, List.of(), null, null);
        for (CategoryReport row : report.getRows()) {
            Category category = row.getCategory();
            if (category.getBudget() == null || (categoryName != null && !category.getName().equalsIgnoreCase(categoryName))) {
                continue;
            }
            String label = " бюджета \"" + category.getName() + "\".";
            BigDecimal remaining = row.getRemainingBudget();
            if (remaining.signum() < 0) {
                io.println("Предупреждение: превышен лимит" + label + " Перерасход: " + money(remaining.negate()));
            } else if (remaining.signum() == 0) {
                io.println("Предупреждение: исчерпан лимит" + label);
            } else if (row.isNearBudget()) {
                io.println("Предупреждение: израсходовано не менее " + CategoryReport.WARNING_PERCENT + "%" + label
                        + " Осталось: " + money(remaining));
            }
        }
        BigDecimal balance = report.getBalance();
        if (balance.signum() < 0) {
            io.println("Предупреждение: расходы превысили доходы. Баланс: " + money(balance));
        } else if (balance.signum() == 0 && report.getTransactionCount() > 0) {
            io.println("Предупреждение: баланс равен нулю.");
        }
    }

    static String money(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
