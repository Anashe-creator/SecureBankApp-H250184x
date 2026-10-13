package com.securebank.util;

import com.securebank.exception.StorageException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Small helpers for reading and writing the plain-text data files safely.
 * Rewrites are atomic (write to a temp file, then move it into place) so a
 * crash cannot leave a half-written data file behind.
 */
public final class FileUtil {

    private static final Set<PosixFilePermission> OWNER_ONLY =
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private FileUtil() {
    }

    /** Creates the file with a comment header if it does not exist yet. */
    public static void ensureFile(Path file, String header) throws StorageException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            if (!Files.exists(file)) {
                Files.write(file, List.of("# " + header), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
                restrictPermissions(file);
            }
        } catch (IOException e) {
            throw new StorageException("Could not prepare data file: " + file.getFileName(), e);
        }
    }

    /** Reads all data lines, skipping blank lines and '#' comments. */
    public static List<String> readDataLines(Path file) throws StorageException {
        try {
            List<String> result = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    result.add(trimmed);
                }
            }
            return result;
        } catch (IOException e) {
            throw new StorageException("Could not read data file: " + file.getFileName(), e);
        }
    }

    /** Atomically replaces the file's content with a header plus the given lines. */
    public static void writeAtomically(Path file, String header, List<String> lines)
            throws StorageException {
        Path abs = file.toAbsolutePath();
        Path temp = null;
        try {
            temp = Files.createTempFile(abs.getParent(), abs.getFileName().toString(), ".tmp");
            List<String> all = new ArrayList<>();
            all.add("# " + header);
            all.addAll(lines);
            Files.write(temp, all, StandardCharsets.UTF_8);
            restrictPermissions(temp);
            try {
                Files.move(temp, abs, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, abs, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // best effort clean-up
                }
            }
            throw new StorageException("Could not write data file: " + file.getFileName(), e);
        }
    }

    public static void appendLine(Path file, String line) throws StorageException {
        try {
            Files.write(file, List.of(line), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new StorageException("Could not write data file: " + file.getFileName(), e);
        }
    }

    /** Restricts the file to its owner where the OS supports POSIX permissions. */
    private static void restrictPermissions(Path file) {
        try {
            Files.setPosixFilePermissions(file, OWNER_ONLY);
        } catch (UnsupportedOperationException | IOException ignored) {
            // e.g. Windows - the file keeps the folder's default permissions
        }
    }
}
