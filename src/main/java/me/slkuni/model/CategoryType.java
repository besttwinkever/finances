package me.slkuni.model;

public enum CategoryType {
    INCOME("Входящий перевод"),
    EXPENSE("Исходящий перевод");

    private final String transferCategory;

    CategoryType(String transferCategory) {
        this.transferCategory = transferCategory;
    }

    public String getTransferCategory() {
        return transferCategory;
    }
}
