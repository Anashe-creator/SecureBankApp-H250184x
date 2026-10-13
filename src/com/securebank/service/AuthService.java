package com.securebank.service;

import com.securebank.exception.AuthenticationException;
import com.securebank.exception.StorageException;
import com.securebank.exception.ValidationException;
import com.securebank.model.User;
import com.securebank.repository.UserRepository;
import com.securebank.util.InputValidator;
import com.securebank.util.PasswordHasher;

/**
 * Handles registration and login. Security measures:
 * salted PBKDF2 hashing, constant-time comparison, a generic error message that
 * does not reveal whether the username or the password was wrong, equalised
 * timing for unknown usernames, and a temporary lock after repeated failures.
 */
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final long LOCK_DURATION_MILLIS = 5L * 60L * 1000L;

    private static final String GENERIC_FAILURE = "Invalid username or password.";

    private static final byte[] DUMMY_SALT = PasswordHasher.generateSalt();
    private static final byte[] DUMMY_HASH = new byte[32];

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public synchronized void register(String usernameInput, char[] password)
            throws ValidationException, StorageException {
        String username = InputValidator.validateUsername(usernameInput);
        InputValidator.validatePassword(password, username);
        if (users.exists(username)) {
            throw new ValidationException("That username is not available.");
        }
        byte[] salt = PasswordHasher.generateSalt();
        int iterations = PasswordHasher.DEFAULT_ITERATIONS;
        byte[] hash = PasswordHasher.hash(password, salt, iterations);
        users.add(new User(username, salt, hash, iterations, 0, 0L));
    }

    /** Verifies credentials and returns the canonical username on success. */
    public synchronized String login(String usernameInput, char[] password)
            throws AuthenticationException, StorageException {
        String username;
        try {
            username = InputValidator.validateUsername(usernameInput);
        } catch (ValidationException e) {
            burnTime(password);
            throw new AuthenticationException(GENERIC_FAILURE);
        }

        User user = users.findByUsername(username);
        if (user == null) {
            burnTime(password);
            throw new AuthenticationException(GENERIC_FAILURE);
        }

        long now = System.currentTimeMillis();
        if (user.isLocked(now)) {
            long minutes = (user.getLockedUntilMillis() - now + 59_999L) / 60_000L;
            throw new AuthenticationException("Too many failed attempts. Try again in about "
                    + minutes + " minute(s).");
        }

        boolean ok = PasswordHasher.verify(password, user.getSalt(), user.getPasswordHash(),
                user.getIterations());
        if (ok) {
            if (user.getFailedAttempts() > 0 || user.getLockedUntilMillis() != 0L) {
                user.clearFailures();
                users.update();
            }
            return username;
        }

        user.recordFailedAttempt();
        if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockUntil(now + LOCK_DURATION_MILLIS);
            user.resetFailedAttempts();
        }
        users.update();
        throw new AuthenticationException(GENERIC_FAILURE);
    }

    /** Performs a hash so that unknown usernames take as long as real ones. */
    private static void burnTime(char[] password) {
        PasswordHasher.verify(password == null ? new char[0] : password, DUMMY_SALT, DUMMY_HASH,
                PasswordHasher.DEFAULT_ITERATIONS);
    }
}
