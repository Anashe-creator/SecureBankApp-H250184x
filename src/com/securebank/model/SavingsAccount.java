package com.securebank.model;

import com.securebank.exception.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;

/** Savings account: lower per-transaction withdrawal limit. */
public class SavingsAccount extends Account {

    private static final BigDecimal WITHDRAWAL_LIMIT = new BigDecimal("5000.00");

    public SavingsAccount(String accountNumber, String ownerUsername,
                          BigDecimal openingBalance, Instant createdAt) throws ValidationException {
        super(accountNumber, ownerUsername, openingBalance, createdAt);
    }

    @Override
    public AccountType getType() {
        return AccountType.SAVINGS;
    }

    @Override
    public BigDecimal getMaxWithdrawalPerTransaction() {
        return WITHDRAWAL_LIMIT;
    }
}
