package com.securebank.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable record of one movement of money on an account. */
public final class Transaction {

    private final String id;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final Instant timestamp;
    private final String description;

    public Transaction(String id, String accountNumber, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, Instant timestamp, String description) {
        this.id = Objects.requireNonNull(id);
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.type = Objects.requireNonNull(type);
        this.amount = Objects.requireNonNull(amount);
        this.balanceAfter = Objects.requireNonNull(balanceAfter);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.description = Objects.requireNonNull(description);
    }

    /** Creates a new transaction with a random id and the current time. */
    public static Transaction create(String accountNumber, TransactionType type, BigDecimal amount,
                                     BigDecimal balanceAfter, String description) {
        return new Transaction(UUID.randomUUID().toString(), accountNumber, type, amount,
                balanceAfter, Instant.now(), description);
    }

    public String getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Instant getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
}
