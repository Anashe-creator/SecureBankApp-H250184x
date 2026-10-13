package com.securebank.exception;

/** Thrown when a data file cannot be read, written or parsed. */
public class StorageException extends BankException {
    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
