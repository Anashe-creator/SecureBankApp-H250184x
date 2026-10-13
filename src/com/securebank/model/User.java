package com.securebank.model;

import java.util.Objects;

/**
 * A bank customer's login record. Only a salted PBKDF2 hash of the password is
 * kept - the password itself is never stored. Also tracks failed logins so the
 * account can be temporarily locked.
 */
public class User {

    private final String username;
    private final byte[] salt;
    private final byte[] passwordHash;
    private final int iterations;
    private int failedAttempts;
    private long lockedUntilMillis;

    public User(String username, byte[] salt, byte[] passwordHash, int iterations,
                int failedAttempts, long lockedUntilMillis) {
        this.username = Objects.requireNonNull(username);
        this.salt = Objects.requireNonNull(salt).clone();
        this.passwordHash = Objects.requireNonNull(passwordHash).clone();
        this.iterations = iterations;
        this.failedAttempts = failedAttempts;
        this.lockedUntilMillis = lockedUntilMillis;
    }

    public String getUsername() { return username; }
    public byte[] getSalt() { return salt.clone(); }
    public byte[] getPasswordHash() { return passwordHash.clone(); }
    public int getIterations() { return iterations; }
    public int getFailedAttempts() { return failedAttempts; }
    public long getLockedUntilMillis() { return lockedUntilMillis; }

    public boolean isLocked(long nowMillis) {
        return nowMillis < lockedUntilMillis;
    }

    public void recordFailedAttempt() {
        failedAttempts++;
    }

    public void lockUntil(long untilMillis) {
        this.lockedUntilMillis = untilMillis;
    }

    /** Resets only the counter (used when a lock starts); the lock time is kept. */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    /** Clears both the counter and any lock (used after a successful login). */
    public void clearFailures() {
        this.failedAttempts = 0;
        this.lockedUntilMillis = 0L;
    }
}
