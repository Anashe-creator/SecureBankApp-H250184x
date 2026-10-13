package com.securebank.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Password hashing with PBKDF2-HMAC-SHA256 and a unique random salt per user.
 * Comparison is done in constant time to avoid timing side channels.
 */
public final class PasswordHasher {

    /** Iteration count recommended by OWASP for PBKDF2-HMAC-SHA256. */
    public static final int DEFAULT_ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available on this JVM.", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verify(char[] password, byte[] salt, byte[] expectedHash, int iterations) {
        byte[] actual = hash(password, salt, iterations);
        try {
            return MessageDigest.isEqual(actual, expectedHash);
        } finally {
            Arrays.fill(actual, (byte) 0);
        }
    }
}
