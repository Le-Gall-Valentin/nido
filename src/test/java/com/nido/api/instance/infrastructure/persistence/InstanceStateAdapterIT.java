package com.nido.api.instance.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.KeyFingerprint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Types;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Works on the shared database's single row, and puts it back afterwards: every other test of the run
 * starts with the row the seed and the encryption key left there.
 */
@IntegrationTestConfig
class InstanceStateAdapterIT {

    @Autowired InstanceStateAdapter adapter;
    @Autowired JdbcClient jdbc;

    private Object[] saved;

    @BeforeEach
    void keep() {
        saved = jdbc.sql("SELECT setup_completed_at, key_fingerprint, key_fingerprint_salt, key_generated FROM instance WHERE id = 1")
            .query((rs, n) -> new Object[]{rs.getObject(1), rs.getBytes(2), rs.getBytes(3), rs.getBoolean(4)}).single();
    }

    @AfterEach
    void restore() {
        jdbc.sql("""
                UPDATE instance SET setup_completed_at = :done, key_fingerprint = :hash,
                                    key_fingerprint_salt = :salt, key_generated = :generated WHERE id = 1
                """)
            .param("done", saved[0], Types.TIMESTAMP_WITH_TIMEZONE)
            .param("hash", saved[1], Types.BINARY)
            .param("salt", saved[2], Types.BINARY)
            .param("generated", saved[3])
            .update();
    }

    @Test
    void the_setup_ends_once() {
        jdbc.sql("UPDATE instance SET setup_completed_at = NULL").update();

        assertThat(adapter.load().setupCompleted()).isFalse();
        assertThat(adapter.markSetupCompleted(Instant.now())).isTrue();
        assertThat(adapter.markSetupCompleted(Instant.now())).isFalse();
        assertThat(adapter.load().setupCompleted()).isTrue();
    }

    @Test
    void a_recorded_fingerprint_reads_back_and_still_recognises_its_key() {
        KeyFingerprint fingerprint = KeyFingerprint.of("a-key-of-at-least-thirty-two-characters");

        adapter.recordFingerprint(fingerprint, true);

        InstanceState state = adapter.load();
        assertThat(state.fingerprint()).contains(fingerprint);
        assertThat(state.keyGenerated()).isTrue();
        assertThat(state.fingerprint().orElseThrow().matches("a-key-of-at-least-thirty-two-characters")).isTrue();
    }
}
