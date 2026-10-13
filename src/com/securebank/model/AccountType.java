package com.securebank.model;

/** The kinds of bank account a customer can open. */
public enum AccountType {
    SAVINGS("Savings"),
    CURRENT("Current");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
