package com.nido.api.instance.infrastructure.secrets;

import com.nido.api.infrastructure.config.DataDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionKeyFileAdapterTest {

    @TempDir Path data;

    private EncryptionKeyFileAdapter adapter() {
        return new EncryptionKeyFileAdapter(new DataDirectory(data));
    }

    @Test
    void the_key_lives_in_secrets_encryption_key_of_the_data_directory() {
        assertThat(adapter().location()).isEqualTo(data.resolve("secrets/encryption-key").toString());
    }

    @Test
    void nothing_is_read_before_a_key_is_created_and_the_created_key_is_read_back() throws Exception {
        EncryptionKeyFileAdapter adapter = adapter();
        assertThat(adapter.read()).isEmpty();

        String key = adapter.create();

        assertThat(key).hasSize(44);
        assertThat(adapter.read()).contains(key);
        assertThat(Files.readString(data.resolve("secrets/encryption-key")).strip()).isEqualTo(key);
    }

    @Test
    void a_key_is_never_overwritten() {
        EncryptionKeyFileAdapter adapter = adapter();
        adapter.create();

        assertThatThrownBy(adapter::create).hasMessageContaining("never overwritten");
    }
}
