package com.securebank.service;

import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.BankException;
import com.securebank.exception.StorageException;
import com.securebank.exception.ValidationException;
import com.securebank.model.Account;
import com.securebank.model.AccountFactory;
import com.securebank.model.AccountType;
import com.securebank.model.Transaction;
import com.securebank.model.TransactionType;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.TransactionRepository;
import com.securebank.util.InputValidator;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Banking operations. Every method takes the logged-in username and checks that
 * the account belongs to that user, so one customer can never read or change
 * another customer's account. State is saved to disk after every change; if
 * saving fails the in-memory change is rolled back.
 */
public class BankService {

    public static final int MAX_ACCOUNTS_PER_USER = 5;

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final SecureRandom random = new SecureRandom();

    public BankService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public synchronized Account createAccount(String username, AccountType type,
                                              BigDecimal initialDeposit) throws BankException {
        if (accounts.findByOwner(username).size() >= MAX_ACCOUNTS_PER_USER) {
            throw new ValidationException("You can have at most " + MAX_ACCOUNTS_PER_USER
                    + " accounts.");
        }
        if (initialDeposit == null || initialDeposit.signum() < 0
                || initialDeposit.compareTo(Account.MAX_TRANSACTION) > 0) {
            throw new ValidationException("Initial deposit must be between 0 and "
                    + Account.MAX_TRANSACTION.toPlainString() + ".");
        }

        Account account = AccountFactory.create(type, generateAccountNumber(), username,
                initialDeposit, Instant.now());
        accounts.add(account);
        try {
            accounts.saveAll();
            transactions.append(Transaction.create(account.getAccountNumber(),
                    TransactionType.ACCOUNT_OPENING, account.getBalance(), account.getBalance(),
                    "Account opened"));
        } catch (StorageException e) {
            accounts.remove(account.getAccountNumber());
            tryResave();
            throw e;
        }
        return account;
    }

    public synchronized List<Account> getAccounts(String username) {
        return Collections.unmodifiableList(accounts.findByOwner(username));
    }

    public synchronized BigDecimal getBalance(String username, String accountNumber)
            throws BankException {
        return getOwnedAccount(username, accountNumber).getBalance();
    }

    public synchronized Transaction deposit(String username, String accountNumber, BigDecimal amount)
            throws BankException {
        Account account = getOwnedAccount(username, accountNumber);
        BigDecimal previous = account.getBalance();
        account.deposit(amount);
        return persistChange(account, previous, TransactionType.DEPOSIT, amount, "Deposit");
    }

    public synchronized Transaction withdraw(String username, String accountNumber, BigDecimal amount)
            throws BankException {
        Account account = getOwnedAccount(username, accountNumber);
        BigDecimal previous = account.getBalance();
        account.withdraw(amount);
        return persistChange(account, previous, TransactionType.WITHDRAWAL, amount, "Withdrawal");
    }

    public synchronized List<Transaction> getHistory(String username, String accountNumber)
            throws BankException {
        getOwnedAccount(username, accountNumber);
        return Collections.unmodifiableList(transactions.findByAccount(accountNumber));
    }

    // ------------------------------------------------------------------

    private Account getOwnedAccount(String username, String accountNumber) throws BankException {
        String number = InputValidator.validateAccountNumber(accountNumber);
        Account account = accounts.findByNumber(number);
        // Same message whether it is missing or belongs to someone else.
        if (account == null || !account.getOwnerUsername().equals(username)) {
            throw new AccountNotFoundException("Account not found.");
        }
        return account;
    }

    private Transaction persistChange(Account account, BigDecimal previousBalance,
                                      TransactionType type, BigDecimal amount, String description)
            throws StorageException {
        Transaction tx = Transaction.create(account.getAccountNumber(), type,
                amount.setScale(2), account.getBalance(), description);
        try {
            accounts.saveAll();
            transactions.append(tx);
        } catch (StorageException e) {
            account.restoreBalance(previousBalance);
            tryResave();
            throw e;
        }
        return tx;
    }

    private void tryResave() {
        try {
            accounts.saveAll();
        } catch (StorageException ignored) {
            // nothing more can be done; the original error is reported to the caller
        }
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = Long.toString(1_000_000_000L + (long) (random.nextDouble() * 9_000_000_000L));
        } while (accounts.exists(number));
        return number;
    }
}
