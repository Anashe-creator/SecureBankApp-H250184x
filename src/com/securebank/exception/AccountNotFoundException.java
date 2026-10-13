package com.securebank.exception;

/** Thrown when an account does not exist or is not owned by the caller. */
public class AccountNotFoundException extends BankException {
    private static final long serialVersionUID = 1L;

    public AccountNotFoundException(String message) {
        super(message);
    }
}
