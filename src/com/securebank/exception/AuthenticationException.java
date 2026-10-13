package com.securebank.exception;

/** Thrown when login fails or an account is locked. */
public class AuthenticationException extends BankException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
