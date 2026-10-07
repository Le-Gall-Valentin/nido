package com.nido.api.infrastructure.sealing;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class SpaceKeyGuardIT {

    private static final UUID A = UUID.randomUUID();
    private static final TextEncryptor KEY = Encryptors.delux(TestSpaces.ENCRYPTION_KEY, "0123456789abcdef0123456789abcdef");
    private static final TextEncryptor WRONG = Encryptors.delux("a-key-someone-typed-by-mistake-32-chars+", "0123456789abcdef0123456789abcdef");
    private static final SealedColumn LABEL = SealedColumn.ofSpace("guard_probes", "label_encrypted");
    private static final SealedColumn NOTE = SealedColumn.ofSpace("guard_probes", "note_encrypted");

    @Autowired JdbcClient jdbc;

    @BeforeEach
    void scratchTable() {
        drop();
        jdbc.sql("CREATE TABLE guard_probes (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), space_id UUID NOT NULL, label_encrypted TEXT, note_encrypted TEXT)").update();
        jdbc.sql("CREATE INDEX ON guard_probes (space_id)").update();
    }

    @AfterEach
    void drop() {
        jdbc.sql("DROP TABLE IF EXISTS guard_probes").update();
    }

    private SpaceKeyGuard guard(TextEncryptor key) {
        return new SpaceKeyGuard(jdbc, space -> SpaceSealer.of(key), List.of(SealedColumns.of(LABEL, NOTE)));
    }

    private void row(String label, String note) {
        row(UUID.randomUUID(), label, note);
    }

    private void row(UUID id, String label, String note) {
        jdbc.sql("INSERT INTO guard_probes (id, space_id, label_encrypted, note_encrypted) VALUES (:id, :s, :l, :n)")
            .param("id", id).param("s", A).param("l", label).param("n", note).update();
    }

    @Test
    void the_right_key_passes_on_old_and_sealed_values() {
        row(KEY.encrypt("Loyer"), null);
        UUID id = jdbc.sql("SELECT id FROM guard_probes").query(UUID.class).single();
        jdbc.sql("UPDATE guard_probes SET note_encrypted = :v").param("v", SpaceSealer.of(KEY).seal(NOTE, id, "Note")).update();

        assertThatCode(guard(KEY)::verify).doesNotThrowAnyException();
    }

    @Test
    void a_wrong_key_is_refused_on_old_values_and_on_sealed_ones() {
        row(KEY.encrypt("Loyer"), null);
        assertThatThrownBy(guard(WRONG)::verify).hasMessageContaining("does not decrypt the data already encrypted in space " + A);
        drop();
        scratchTable();
        row(SpaceSealer.of(KEY).seal(LABEL, UUID.randomUUID(), "Loyer"), null);
        assertThatThrownBy(guard(WRONG)::verify).hasMessageContaining("does not decrypt");
    }

    @Test
    void a_space_whose_only_encrypted_value_is_in_another_column_is_checked_too() {
        row(null, KEY.encrypt("Note"));

        assertThatCode(guard(KEY)::verify).doesNotThrowAnyException();
        assertThatThrownBy(guard(WRONG)::verify).hasMessageContaining("does not decrypt the data already encrypted in space " + A);
    }

    @Test
    void one_damaged_value_among_good_ones_is_not_taken_for_a_wrong_key() {
        // The damaged one sorts first: it is sampled whatever the number of samples.
        row(UUID.fromString("00000000-0000-4000-8000-000000000001"), "not-a-ciphertext", null);
        row(UUID.fromString("00000000-0000-4000-8000-000000000002"), KEY.encrypt("Loyer"), null);
        row(UUID.fromString("00000000-0000-4000-8000-000000000003"), SpaceSealer.of(KEY).seal(LABEL, UUID.randomUUID(), "moved here"), null);

        assertThatCode(guard(KEY)::verify).doesNotThrowAnyException();
    }

    @Test
    void a_sealed_value_that_opens_but_belongs_elsewhere_still_proves_the_key() {
        // Only the right key opens it at all: where it belongs is the reading's business, not the key check's.
        row(SpaceSealer.of(KEY).seal(LABEL, UUID.randomUUID(), "moved here"), null);

        assertThatCode(guard(KEY)::verify).doesNotThrowAnyException();
    }

    @Test
    void a_key_that_cannot_be_derived_is_not_reported_as_a_wrong_key() {
        row(KEY.encrypt("Loyer"), null);
        SpaceKeyGuard guard = new SpaceKeyGuard(jdbc, space -> {
            throw new IllegalArgumentException("Detected a Non-hex character");
        }, List.of(SealedColumns.of(LABEL, NOTE)));

        assertThatThrownBy(guard::verify)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Could not derive the key of space " + A)
            .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain("does not decrypt"));
    }
}
