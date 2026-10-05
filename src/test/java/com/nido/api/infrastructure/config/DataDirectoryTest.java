package com.nido.api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

class DataDirectoryTest {

    @TempDir Path root;

    @Test
    void a_created_secret_is_32_random_bytes_in_base64_and_reads_back() {
        DataDirectory directory = new DataDirectory(root);

        String secret = directory.createSecret("jwt-secret");

        assertThat(Base64.getDecoder().decode(secret)).hasSize(32);
        assertThat(directory.readSecret("jwt-secret")).contains(secret);
        assertThat(directory.secretPath("jwt-secret")).isEqualTo(root.toAbsolutePath().normalize().resolve("secrets/jwt-secret"));
    }

    @Test
    void two_secrets_are_never_the_same() {
        DataDirectory directory = new DataDirectory(root);

        assertThat(directory.createSecret("a")).isNotEqualTo(directory.createSecret("b"));
    }

    @Test
    void a_secret_is_never_overwritten() {
        DataDirectory directory = new DataDirectory(root);
        directory.createSecret("encryption-key");

        assertThatThrownBy(() -> directory.createSecret("encryption-key"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("never overwritten");
    }

    @Test
    void read_or_create_creates_once_then_reads() {
        DataDirectory directory = new DataDirectory(root);

        String first = directory.readOrCreateSecret("jwt-secret");

        assertThat(directory.readOrCreateSecret("jwt-secret")).isEqualTo(first);
    }

    @Test
    void a_missing_secret_reads_as_empty() {
        assertThat(new DataDirectory(root).readSecret("jwt-secret")).isEmpty();
    }

    @Test
    void a_secret_file_and_its_directory_are_readable_by_their_owner_only() throws Exception {
        assumeThat(root.getFileSystem().supportedFileAttributeViews()).contains("posix");
        DataDirectory directory = new DataDirectory(root);
        directory.createSecret("jwt-secret");

        assertThat(Files.getPosixFilePermissions(directory.secretPath("jwt-secret")))
            .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        assertThat(Files.getPosixFilePermissions(root.resolve("secrets")))
            .isEqualTo(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
    }

    @Test
    void no_temporary_file_is_left_behind() throws Exception {
        new DataDirectory(root).createSecret("jwt-secret");

        try (var files = Files.list(root.resolve("secrets"))) {
            assertThat(files.map(p -> p.getFileName().toString())).containsExactly("jwt-secret");
        }
    }

    @Test
    void an_empty_secret_file_is_refused_rather_than_used() throws Exception {
        Files.createDirectories(root.resolve("secrets"));
        Files.writeString(root.resolve("secrets/encryption-key"), "\n");

        assertThatThrownBy(() -> new DataDirectory(root).readSecret("encryption-key"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("is empty");
    }

    @Test
    void a_directory_that_cannot_be_written_stops_with_a_message_naming_the_variable() throws Exception {
        Path notADirectory = Files.writeString(root.resolve("file"), "x");

        assertThatThrownBy(() -> new DataDirectory(notADirectory).createSecret("jwt-secret"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NIDO_DATA_DIR");
    }
}
