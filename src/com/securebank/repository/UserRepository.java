package com.securebank.repository;

import com.securebank.exception.StorageException;
import com.securebank.model.User;
import com.securebank.util.FileUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores users in a text file. Line format:
 * username|salt(Base64)|hash(Base64)|iterations|failedAttempts|lockedUntilMillis
 */
public class UserRepository {

    private static final String HEADER =
            "SecureBankApp users: username|salt|passwordHash|iterations|failedAttempts|lockedUntil";

    private final Path file;
    private final Map<String, User> users = new LinkedHashMap<>();

    public UserRepository(Path file) throws StorageException {
        this.file = file;
        FileUtil.ensureFile(file, HEADER);
        load();
    }

    private void load() throws StorageException {
        int lineNo = 0;
        for (String line : FileUtil.readDataLines(file)) {
            lineNo++;
            String[] p = line.split("\\|", -1);
            if (p.length != 6) {
                throw new StorageException("Corrupted users file (entry " + lineNo + ").");
            }
            try {
                User u = new User(p[0],
                        Base64.getDecoder().decode(p[1]),
                        Base64.getDecoder().decode(p[2]),
                        Integer.parseInt(p[3]),
                        Integer.parseInt(p[4]),
                        Long.parseLong(p[5]));
                if (u.getIterations() < 1) {
                    throw new IllegalArgumentException("iterations");
                }
                users.put(u.getUsername(), u);
            } catch (IllegalArgumentException e) {
                throw new StorageException("Corrupted users file (entry " + lineNo + ").", e);
            }
        }
    }

    public synchronized User findByUsername(String username) {
        return users.get(username);
    }

    public synchronized boolean exists(String username) {
        return users.containsKey(username);
    }

    public synchronized void add(User user) throws StorageException {
        users.put(user.getUsername(), user);
        try {
            save();
        } catch (StorageException e) {
            users.remove(user.getUsername());
            throw e;
        }
    }

    /** Persists changes made to an existing user (e.g. failed-login counters). */
    public synchronized void update() throws StorageException {
        save();
    }

    private void save() throws StorageException {
        Base64.Encoder enc = Base64.getEncoder();
        List<String> lines = new ArrayList<>();
        for (User u : users.values()) {
            lines.add(u.getUsername() + "|" + enc.encodeToString(u.getSalt()) + "|"
                    + enc.encodeToString(u.getPasswordHash()) + "|" + u.getIterations() + "|"
                    + u.getFailedAttempts() + "|" + u.getLockedUntilMillis());
        }
        FileUtil.writeAtomically(file, HEADER, lines);
    }
}
