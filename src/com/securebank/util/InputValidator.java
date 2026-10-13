package com.securebank.util;

import com.securebank.exception.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Central place for validating everything a user types. Uses allow-lists
 * (what is permitted) rather than block-lists (what is forbidden).
 */
public final class InputValidator {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 64;

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("^[0-9]{10}$");
    private static final Pattern AMOUNT = Pattern.compile("^[0-9]{1,9}(\\.[0-9]{1,2})?$");

    private InputValidator() {
    }

    /** Returns the username in its canonical (lower-case) form. */
    public static String validateUsername(String input) throws ValidationException {
        if (input == null || !USERNAME.matcher(input.trim()).matches()) {
            throw new ValidationException(
                    "Username must be 3-20 characters: letters, digits or underscore only.");
        }
        return input.trim().toLowerCase();
    }

    public static void validatePassword(char[] password, String username) throws ValidationException {
        java.util.List<String> problems = new java.util.ArrayList<>();
        int length = password == null ? 0 : password.length;
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            problems.add("must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH
                    + " characters long (you entered " + length + ")");
        }
        boolean upper = false, lower = false, digit = false, special = false, space = false;
        if (password != null) {
            for (char c : password) {
                if (Character.isWhitespace(c)) {
                    space = true;
                } else if (Character.isUpperCase(c)) {
                    upper = true;
                } else if (Character.isLowerCase(c)) {
                    lower = true;
                } else if (Character.isDigit(c)) {
                    digit = true;
                } else {
                    special = true;
                }
            }
        }
        if (!upper) problems.add("needs at least one UPPER-case letter");
        if (!lower) problems.add("needs at least one lower-case letter");
        if (!digit) problems.add("needs at least one digit (0-9)");
        if (!special) problems.add("needs at least one special character (e.g. @ # $ ! %)");
        if (space) problems.add("must not contain spaces");
        if (password != null && username != null && new String(password).equalsIgnoreCase(username)) {
            problems.add("must not be the same as the username");
        }
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder("Password is not strong enough:");
            for (String problem : problems) {
                sb.append("\n   - ").append(problem);
            }
            throw new ValidationException(sb.toString());
        }
    }

    public static String validateAccountNumber(String input) throws ValidationException {
        if (input == null || !ACCOUNT_NUMBER.matcher(input.trim()).matches()) {
            throw new ValidationException("Account number must be exactly 10 digits.");
        }
        return input.trim();
    }

    /** Parses a money amount such as "250" or "250.75" (no signs, no exponents). */
    public static BigDecimal parseAmount(String input) throws ValidationException {
        if (input == null || !AMOUNT.matcher(input.trim()).matches()) {
            throw new ValidationException(
                    "Enter a valid amount, e.g. 150 or 150.75 (max 2 decimal places).");
        }
        return new BigDecimal(input.trim()).setScale(2);
    }
}
