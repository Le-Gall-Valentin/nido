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
        jdbc.sql("INSERT INTO guard_probes (space_id, label_encrypted, note_encrypted) VALUES (:s, :l, :n)")
            .param("s", A).param("l", label).param("n", note).update();
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
        row("not-a-ciphertext", null);
        row(KEY.encrypt("Loyer"), null);
        row(SpaceSealer.of(KEY).seal(LABEL, UUID.randomUUID(), "moved here"), null);

        assertThatCode(guard(KEY)::verify).doesNotThrowAnyException();
    }
}
