package com.nido.api.infrastructure.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Where Nido keeps what it generates for itself: the secrets an installation did not provide.
 * NIDO_DATA_DIR — /data in the image, which declares it a volume — and ./data elsewhere.
 *
 * <p>A secret is written once and never rewritten: to a temporary file in the same directory, then
 * moved into place in one step, so a crash may leave a stray temporary file but never half a key. It
 * is readable by its owner only. A directory that cannot be written stops the start: a secret that
 * vanished with the container would be replaced by a different one at the next start.
 */
public final class DataDirectory {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SECRET_BYTES = 32;

    private final Path secrets;

    public DataDirectory(Path root) {
        this.secrets = root.toAbsolutePath().normalize().resolve("secrets");
    }

    public Path secretPath(String name) {
        return secrets.resolve(name);
    }

    public Optional<String> readSecret(String name) {
        Path file = secretPath(name);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            String value = Files.readString(file, StandardCharsets.UTF_8).strip();
            if (value.isEmpty()) {
                throw new IllegalStateException(file + " is empty: restore it from a backup");
            }
            return Optional.of(value);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file + " — check NIDO_DATA_DIR and who owns it", e);
        }
    }

    public String createSecret(String name) {
        Path file = secretPath(name);
        if (Files.exists(file)) {
            throw new IllegalStateException(file + " already exists, and a secret is never overwritten");
        }
        String value = newSecret();
        try {
            createPrivateDirectory(secrets);
            Path temporary = createPrivateFile(secrets, name);
            Files.writeString(temporary, value + "\n", StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + file + " — NIDO_DATA_DIR must be a writable directory "
                + "that survives restarts, such as a Docker volume", e);
        }
        return value;
    }

    public String readOrCreateSecret(String name) {
        return readSecret(name).orElseGet(() -> createSecret(name));
    }

    static String newSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static boolean posix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    private static void createPrivateDirectory(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            return;
        }
        if (posix()) {
            Files.createDirectories(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        } else {
            Files.createDirectories(directory);
        }
    }

    private static Path createPrivateFile(Path directory, String name) throws IOException {
        return posix()
            ? Files.createTempFile(directory, name + "-", ".tmp",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
            : Files.createTempFile(directory, name + "-", ".tmp");
    }
}
