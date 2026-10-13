package com.securebank.model;

import com.securebank.exception.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;

/** Current (cheque) account: higher per-transaction withdrawal limit. */
public class CurrentAccount extends Account {

    private static final BigDecimal WITHDRAWAL_LIMIT = new BigDecimal("20000.00");

    public CurrentAccount(String accountNumber, String ownerUsername,
                          BigDecimal openingBalance, Instant createdAt) throws ValidationException {
        super(accountNumber, ownerUsername, openingBalance, createdAt);
    }

    @Override
    public AccountType getType() {
        return AccountType.CURRENT;
    }

    @Override
    public BigDecimal getMaxWithdrawalPerTransaction() {
        return WITHDRAWAL_LIMIT;
    }
}
