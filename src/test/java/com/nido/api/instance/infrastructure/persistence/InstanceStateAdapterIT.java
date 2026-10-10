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
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Works on the shared database's single row, and puts it back afterwards: every other test of the run
 * starts with the row the seed and the encryption key left there. A row left with another key's
 * fingerprint would make every context started afterwards refuse to start — and a context that fails
 * closes the shared containers with it.
 */
@IntegrationTestConfig
class InstanceStateAdapterIT {

    @Autowired InstanceStateAdapter adapter;
    @Autowired JdbcClient jdbc;

    private Object[] saved;

    @BeforeEach
    void keep() {
        saved = jdbc.sql("""
                SELECT setup_completed_at, key_fingerprint, key_fingerprint_salt, key_generated, key_fingerprint_confirmed
                FROM instance WHERE id = 1""")
            .query((rs, n) -> new Object[]{rs.getObject(1, OffsetDateTime.class), rs.getBytes(2), rs.getBytes(3), rs.getBoolean(4),
                rs.getBoolean(5)})
            .single();
    }

    @AfterEach
    void restore() {
        jdbc.sql("""
                UPDATE instance SET setup_completed_at = :done, key_fingerprint = :hash, key_fingerprint_salt = :salt,
                                    key_generated = :generated, key_fingerprint_confirmed = :confirmed WHERE id = 1
                """)
            .param("done", saved[0], Types.TIMESTAMP_WITH_TIMEZONE)
            .param("hash", saved[1], Types.BINARY)
            .param("salt", saved[2], Types.BINARY)
            .param("generated", saved[3])
            .param("confirmed", saved[4])
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
        assertThat(state.fingerprintConfirmed()).as("taken on the data's word until confirmed").isFalse();
    }

    @Test
    void only_the_fingerprint_in_place_is_confirmed_and_only_once() {
        KeyFingerprint fingerprint = KeyFingerprint.of("a-key-of-at-least-thirty-two-characters");
        adapter.recordFingerprint(fingerprint, false);

        assertThat(adapter.confirmFingerprint(KeyFingerprint.of("another-key-of-at-least-thirty-two-chars"))).isFalse();
        assertThat(adapter.load().fingerprintConfirmed()).isFalse();
        assertThat(adapter.confirmFingerprint(fingerprint)).isTrue();
        assertThat(adapter.confirmFingerprint(fingerprint)).isFalse();
        assertThat(adapter.load().fingerprintConfirmed()).isTrue();
    }

    @Test
    void only_the_unconfirmed_fingerprint_in_place_is_forgotten() {
        KeyFingerprint fingerprint = KeyFingerprint.of("a-key-of-at-least-thirty-two-characters");
        adapter.recordFingerprint(fingerprint, true);

        assertThat(adapter.forgetFingerprint(KeyFingerprint.of("another-key-of-at-least-thirty-two-chars"))).isFalse();
        assertThat(adapter.load().fingerprint()).contains(fingerprint);
        assertThat(adapter.forgetFingerprint(fingerprint)).isTrue();
        InstanceState state = adapter.load();
        assertThat(state.fingerprint()).isEmpty();
        assertThat(state.keyGenerated()).isFalse();
    }

    @Test
    void a_confirmed_fingerprint_is_never_forgotten() {
        KeyFingerprint fingerprint = KeyFingerprint.of("a-key-of-at-least-thirty-two-characters");
        adapter.recordFingerprint(fingerprint, false);
        adapter.confirmFingerprint(fingerprint);

        assertThat(adapter.forgetFingerprint(fingerprint)).isFalse();
        assertThat(adapter.load().fingerprint()).contains(fingerprint);
    }

    @Test
    void earlier_formats_close_once_and_stay_closed() {
        // Every context of the run closed them at its first start; the row is left closed again, as found.
        jdbc.sql("UPDATE instance SET legacy_formats_closed_at = NULL WHERE id = 1").update();

        assertThat(adapter.legacyFormatsClosed()).isFalse();
        assertThat(adapter.closeLegacyFormats(Instant.now())).isTrue();
        assertThat(adapter.closeLegacyFormats(Instant.now())).isFalse();
        assertThat(adapter.legacyFormatsClosed()).isTrue();
    }
}
