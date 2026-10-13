package com.securebank.exception;

/** Thrown when user supplied input fails validation. */
public class ValidationException extends BankException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
