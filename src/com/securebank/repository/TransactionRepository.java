package com.securebank.repository;

import com.securebank.exception.StorageException;
import com.securebank.model.Transaction;
import com.securebank.model.TransactionType;
import com.securebank.util.FileUtil;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the transaction history in an append-only text file. Line format:
 * id|accountNumber|TYPE|amount|balanceAfter|timestamp|description
 */
public class TransactionRepository {

    private static final String HEADER =
            "SecureBankApp transactions: id|accountNumber|type|amount|balanceAfter|timestamp|description";

    private final Path file;
    private final List<Transaction> transactions = new ArrayList<>();

    public TransactionRepository(Path file) throws StorageException {
        this.file = file;
        FileUtil.ensureFile(file, HEADER);
        load();
    }

    private void load() throws StorageException {
        int lineNo = 0;
        for (String line : FileUtil.readDataLines(file)) {
            lineNo++;
            String[] p = line.split("\\|", -1);
            if (p.length != 7) {
                throw new StorageException("Corrupted transactions file (entry " + lineNo + ").");
            }
            try {
                transactions.add(new Transaction(p[0], p[1], TransactionType.valueOf(p[2]),
                        new BigDecimal(p[3]), new BigDecimal(p[4]), Instant.parse(p[5]), p[6]));
            } catch (RuntimeException e) {
                throw new StorageException("Corrupted transactions file (entry " + lineNo + ").", e);
            }
        }
    }

    public synchronized void append(Transaction t) throws StorageException {
        String line = t.getId() + "|" + t.getAccountNumber() + "|" + t.getType().name() + "|"
                + t.getAmount().toPlainString() + "|" + t.getBalanceAfter().toPlainString() + "|"
                + t.getTimestamp() + "|" + sanitise(t.getDescription());
        FileUtil.appendLine(file, line);
        transactions.add(t);
    }

    public synchronized List<Transaction> findByAccount(String accountNumber) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getAccountNumber().equals(accountNumber)) {
                result.add(t);
            }
        }
        return result;
    }

    /** Prevents field-delimiter or line-break injection into the data file. */
    private static String sanitise(String text) {
        return text.replaceAll("[|\\r\\n]", " ");
    }
}
