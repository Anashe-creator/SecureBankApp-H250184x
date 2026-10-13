package com.securebank.repository;

import com.securebank.exception.StorageException;
import com.securebank.exception.ValidationException;
import com.securebank.model.Account;
import com.securebank.model.AccountFactory;
import com.securebank.model.AccountType;
import com.securebank.util.FileUtil;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores accounts in a text file. Line format:
 * accountNumber|owner|TYPE|balance|createdAt(ISO-8601)
 */
public class AccountRepository {

    private static final String HEADER =
            "SecureBankApp accounts: accountNumber|owner|type|balance|createdAt";

    private final Path file;
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public AccountRepository(Path file) throws StorageException {
        this.file = file;
        FileUtil.ensureFile(file, HEADER);
        load();
    }

    private void load() throws StorageException {
        int lineNo = 0;
        for (String line : FileUtil.readDataLines(file)) {
            lineNo++;
            String[] p = line.split("\\|", -1);
            if (p.length != 5) {
                throw new StorageException("Corrupted accounts file (entry " + lineNo + ").");
            }
            try {
                Account a = AccountFactory.create(AccountType.valueOf(p[2]), p[0], p[1],
                        new BigDecimal(p[3]), Instant.parse(p[4]));
                accounts.put(a.getAccountNumber(), a);
            } catch (ValidationException | RuntimeException e) {
                throw new StorageException("Corrupted accounts file (entry " + lineNo + ").", e);
            }
        }
    }

    public synchronized Account findByNumber(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public synchronized boolean exists(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    public synchronized List<Account> findByOwner(String username) {
        List<Account> result = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.getOwnerUsername().equals(username)) {
                result.add(a);
            }
        }
        return result;
    }

    public synchronized void add(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public synchronized void remove(String accountNumber) {
        accounts.remove(accountNumber);
    }

    /** Writes every account to disk (atomic replace). */
    public synchronized void saveAll() throws StorageException {
        List<String> lines = new ArrayList<>();
        for (Account a : accounts.values()) {
            lines.add(a.getAccountNumber() + "|" + a.getOwnerUsername() + "|" + a.getType().name()
                    + "|" + a.getBalance().toPlainString() + "|" + a.getCreatedAt());
        }
        FileUtil.writeAtomically(file, HEADER, lines);
    }
}
