package com.securebank.model;

import com.securebank.exception.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;

/** Creates the correct {@link Account} subclass for an {@link AccountType}. */
public final class AccountFactory {

    private AccountFactory() {
    }

    public static Account create(AccountType type, String accountNumber, String ownerUsername,
                                 BigDecimal openingBalance, Instant createdAt)
            throws ValidationException {
        switch (type) {
            case SAVINGS:
                return new SavingsAccount(accountNumber, ownerUsername, openingBalance, createdAt);
            case CURRENT:
                return new CurrentAccount(accountNumber, ownerUsername, openingBalance, createdAt);
            default:
                throw new ValidationException("Unsupported account type.");
        }
    }
}
