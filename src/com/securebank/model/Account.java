package com.securebank.model;

import com.securebank.exception.InsufficientFundsException;
import com.securebank.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Abstract bank account. Money is always held as {@link BigDecimal} with two
 * decimal places (never double) and the balance can only change through
 * {@link #deposit(BigDecimal)} and {@link #withdraw(BigDecimal)}, which enforce
 * the business rules. Concrete account types supply their own limits.
 */
public abstract class Account {

    /** Largest amount allowed in a single deposit or withdrawal. */
    public static final BigDecimal MAX_TRANSACTION = new BigDecimal("1000000.00");
    /** Largest balance an account may hold. */
    public static final BigDecimal MAX_BALANCE = new BigDecimal("999999999.99");

    private final String accountNumber;
    private final String ownerUsername;
    private final Instant createdAt;
    private BigDecimal balance;

    protected Account(String accountNumber, String ownerUsername,
                      BigDecimal openingBalance, Instant createdAt) throws ValidationException {
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.ownerUsername = Objects.requireNonNull(ownerUsername, "ownerUsername");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        BigDecimal start = normalise(openingBalance);
        if (start.signum() < 0 || start.compareTo(MAX_BALANCE) > 0) {
            throw new ValidationException("Opening balance is out of the allowed range.");
        }
        this.balance = start;
    }

    /** The kind of account, e.g. SAVINGS. */
    public abstract AccountType getType();

    /** Largest amount this type of account allows in one withdrawal. */
    public abstract BigDecimal getMaxWithdrawalPerTransaction();

    public synchronized void deposit(BigDecimal amount) throws ValidationException {
        BigDecimal value = requireValidAmount(amount);
        BigDecimal newBalance = balance.add(value);
        if (newBalance.compareTo(MAX_BALANCE) > 0) {
            throw new ValidationException("This deposit would exceed the maximum allowed balance.");
        }
        balance = newBalance;
    }

    public synchronized void withdraw(BigDecimal amount)
            throws ValidationException, InsufficientFundsException {
        BigDecimal value = requireValidAmount(amount);
        if (value.compareTo(getMaxWithdrawalPerTransaction()) > 0) {
            throw new ValidationException("Withdrawal limit for a " + getType().getLabel()
                    + " account is " + getMaxWithdrawalPerTransaction().toPlainString()
                    + " per transaction.");
        }
        if (value.compareTo(balance) > 0) {
            throw new InsufficientFundsException("Insufficient funds for this withdrawal.");
        }
        balance = balance.subtract(value);
    }

    /**
     * Puts the balance back to an earlier value. Used only by the service layer
     * to undo an in-memory change when saving to disk failed.
     */
    public synchronized void restoreBalance(BigDecimal previousBalance) {
        this.balance = normalise(previousBalance);
    }

    public synchronized BigDecimal getBalance() {
        return balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static BigDecimal requireValidAmount(BigDecimal amount) throws ValidationException {
        if (amount == null) {
            throw new ValidationException("Amount is required.");
        }
        BigDecimal value;
        try {
            value = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ValidationException("Amount can have at most 2 decimal places.");
        }
        if (value.signum() <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (value.compareTo(MAX_TRANSACTION) > 0) {
            throw new ValidationException("Amount exceeds the per-transaction limit of "
                    + MAX_TRANSACTION.toPlainString() + ".");
        }
        return value;
    }

    private static BigDecimal normalise(BigDecimal value) {
        return Objects.requireNonNull(value, "amount").setScale(2, RoundingMode.HALF_UP);
    }
}
