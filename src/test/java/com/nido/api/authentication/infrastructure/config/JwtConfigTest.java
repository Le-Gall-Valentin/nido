package com.nido.api.authentication.infrastructure.config;

import com.nido.api.infrastructure.config.DataDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigTest {

    @TempDir Path root;

    @Test
    void a_configured_secret_wins_and_nothing_is_written() {
        DataDirectory directory = new DataDirectory(root);

        assertThat(JwtConfig.secret("configured-secret-of-at-least-32-chars", directory))
            .isEqualTo("configured-secret-of-at-least-32-chars");
        assertThat(Files.exists(directory.secretPath("jwt-secret"))).isFalse();
    }

    @Test
    void without_one_a_secret_is_generated_once_and_kept() {
        DataDirectory directory = new DataDirectory(root);

        String first = JwtConfig.secret("  ", directory);

        assertThat(first).hasSize(44);
        assertThat(JwtConfig.secret(null, directory)).isEqualTo(first);
    }
}
