package com.nido.api.infrastructure.config;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The pieces of the backfill against real Postgres, on scratch tables shaped like the real ones: the
 * shared test database has long dropped its columns in clear.
 */
@IntegrationTestConfig
class EncryptionBackfillPartsIT {

    private static final String KEY = "integration-test-encryption-secret-32chars!";
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final TextEncryptor SPACE_A = Encryptors.delux(KEY, "0123456789abcdef0123456789abcdef");
    private static final TextEncryptor SPACE_B = Encryptors.delux(KEY, "fedcba9876543210fedcba9876543210");

    private static final PlaintextTable PROBES = PlaintextTable.ofSpace("backfill_probes", "label", "note");
    private static final PlaintextTable PROBE_LINES =
        PlaintextTable.throughParent("backfill_probe_lines", "probe_id", "backfill_probes", "text");

    @Autowired JdbcClient jdbc;
    @Autowired PlaintextTableEncryptor encryptor;
    @Autowired TableVacuum vacuum;

    @BeforeEach
    void scratchTables() {
        dropScratchTables();
        jdbc.sql("""
            CREATE TABLE backfill_probes (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), space_id UUID NOT NULL,
              label VARCHAR(60), label_encrypted TEXT, note TEXT, note_encrypted TEXT)""").update();
        jdbc.sql("CREATE INDEX ON backfill_probes (space_id)").update();
        jdbc.sql("""
            CREATE TABLE backfill_probe_lines (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
              probe_id UUID NOT NULL REFERENCES backfill_probes (id), text VARCHAR(2000), text_encrypted TEXT)""").update();
        jdbc.sql("CREATE INDEX ON backfill_probe_lines (probe_id)").update();
    }

    @AfterEach
    void dropScratchTables() {
        jdbc.sql("DROP TABLE IF EXISTS backfill_probe_lines, backfill_probes").update();
    }

    private static TextEncryptor keyOf(UUID space) {
        return space.equals(A) ? SPACE_A : SPACE_B;
    }

    private UUID probe(UUID space, String label, String note) {
        return jdbc.sql("INSERT INTO backfill_probes (space_id, label, note) VALUES (:space, :label, :note) RETURNING id")
            .param("space", space).param("label", label).param("note", note)
            .query(UUID.class).single();
    }

    private Map<String, Object> row(String table, UUID id) {
        return jdbc.sql("SELECT * FROM " + table + " WHERE id = :id").param("id", id).query().singleRow();
    }

    @Test
    void every_value_in_clear_ends_up_encrypted_with_the_key_of_its_space_and_the_clear_column_empty() {
        UUID inA = probe(A, "Crème fraîche 🥛", null);
        UUID inB = probe(B, "Coloc", "");

        assertThat(encryptor.encrypt(PROBES, EncryptionBackfillPartsIT::keyOf)).isEqualTo(2);

        Map<String, Object> a = row("backfill_probes", inA);
        assertThat(a.get("label")).isNull();
        assertThat(SPACE_A.decrypt((String) a.get("label_encrypted"))).isEqualTo("Crème fraîche 🥛");
        assertThat(a.get("note_encrypted")).isNull();
        Map<String, Object> b = row("backfill_probes", inB);
        assertThat(SPACE_B.decrypt((String) b.get("label_encrypted"))).isEqualTo("Coloc");
        assertThat(b.get("note")).isNull();
        assertThat(SPACE_B.decrypt((String) b.get("note_encrypted"))).isEmpty();
    }

    @Test
    void a_child_row_takes_the_key_of_its_parents_space_and_a_long_text_comes_back_intact() {
        UUID parent = probe(B, null, null);
        String longest = "🥚".repeat(1000);
        UUID line = jdbc.sql("INSERT INTO backfill_probe_lines (probe_id, text) VALUES (:p, :t) RETURNING id")
            .param("p", parent).param("t", longest).query(UUID.class).single();

        encryptor.encrypt(PROBE_LINES, EncryptionBackfillPartsIT::keyOf);

        assertThat(SPACE_B.decrypt((String) row("backfill_probe_lines", line).get("text_encrypted"))).isEqualTo(longest);
    }

    @Test
    void more_rows_than_a_batch_are_all_encrypted() {
        int count = PlaintextTableEncryptor.BATCH_SIZE * 2 + 201;
        jdbc.sql("INSERT INTO backfill_probes (space_id, label) SELECT :space, 'Article ' || n FROM generate_series(1, :count) n")
            .param("space", A).param("count", count).update();

        assertThat(encryptor.encrypt(PROBES, EncryptionBackfillPartsIT::keyOf)).isEqualTo(count);
        assertThat(jdbc.sql("SELECT count(*) FROM backfill_probes WHERE label IS NOT NULL").query(Long.class).single()).isZero();
    }

    @Test
    void a_value_in_clear_wins_over_an_older_encrypted_one() {
        UUID id = jdbc.sql("INSERT INTO backfill_probes (space_id, label, label_encrypted) VALUES (:s, 'Nouveau', :old) RETURNING id")
            .param("s", A).param("old", SPACE_A.encrypt("Ancien")).query(UUID.class).single();

        encryptor.encrypt(PROBES, EncryptionBackfillPartsIT::keyOf);

        assertThat(SPACE_A.decrypt((String) row("backfill_probes", id).get("label_encrypted"))).isEqualTo("Nouveau");
    }

    @Test
    void pending_says_whether_anything_is_left_and_a_second_run_changes_nothing() {
        UUID id = probe(A, "Pâtes", null);
        assertThat(encryptor.pending(PROBES)).isTrue();

        encryptor.encrypt(PROBES, EncryptionBackfillPartsIT::keyOf);
        Object once = row("backfill_probes", id).get("label_encrypted");

        assertThat(encryptor.pending(PROBES)).isFalse();
        assertThat(encryptor.encrypt(PROBES, EncryptionBackfillPartsIT::keyOf)).isZero();
        assertThat(row("backfill_probes", id).get("label_encrypted")).isEqualTo(once);
    }

    @Test
    void once_the_columns_in_clear_are_gone_there_is_nothing_to_do() {
        jdbc.sql("ALTER TABLE backfill_probes DROP COLUMN label, DROP COLUMN note").update();

        assertThat(encryptor.pending(PROBES)).isFalse();
        assertThat(encryptor.encrypt(PROBES, EncryptionBackfillPartsIT::keyOf)).isZero();
    }

    @Test
    void a_row_that_cannot_be_encrypted_stops_the_run_and_never_shows_its_value() {
        UUID secret = probe(A, "Secret de famille", null);

        assertThatThrownBy(() -> encryptor.encrypt(PROBES, space -> {
                throw new IllegalArgumentException("Detected a Non-hex character");
            }))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Could not encrypt row " + secret + " of backfill_probes")
            .hasNoCause()
            .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain("Secret de famille"));
        assertThat(row("backfill_probes", secret).get("label")).isEqualTo("Secret de famille");
    }

    @Test
    void the_existing_ciphertext_check_refuses_a_key_that_does_not_decrypt_and_names_the_space() {
        jdbc.sql("INSERT INTO backfill_probes (space_id, label_encrypted) VALUES (:s, :v)")
            .param("s", A).param("v", SPACE_A.encrypt("Loyer")).update();
        String oneValuePerSpace = """
            SELECT DISTINCT ON (space_id) space_id, label_encrypted AS value
            FROM backfill_probes WHERE label_encrypted IS NOT NULL ORDER BY space_id""";

        assertThatCode(new SpaceCiphertextCheck(jdbc, EncryptionBackfillPartsIT::keyOf, "probes", oneValuePerSpace)::verify)
            .doesNotThrowAnyException();
        assertThatThrownBy(new SpaceCiphertextCheck(jdbc, space -> SPACE_B, "probes", oneValuePerSpace)::verify)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does not decrypt the probes already encrypted in space " + A);
    }

    @Test
    void vacuum_full_moves_the_table_to_new_files() {
        probe(A, "Pâtes", null);
        long before = filenode();

        vacuum.vacuumFull(List.of("backfill_probes"));

        assertThat(filenode()).isNotEqualTo(before);
    }

    private long filenode() {
        return jdbc.sql("SELECT pg_relation_filenode('backfill_probes')").query(Long.class).single();
    }
}
