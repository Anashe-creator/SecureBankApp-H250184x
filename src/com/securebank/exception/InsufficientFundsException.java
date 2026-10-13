package com.securebank.exception;

/** Thrown when a withdrawal would break an account rule. */
public class InsufficientFundsException extends BankException {
    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String message) {
        super(message);
    }
}
