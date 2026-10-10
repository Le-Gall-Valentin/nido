package com.nido.api.infrastructure.sealing;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.encryption.DataKeys;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class SealedValueMigrationIT {

    private static final UUID A = UUID.randomUUID();
    private static final String SALT = "0123456789abcdef0123456789abcdef";
    private static final TextEncryptor CURRENT = DataKeys.current(new EncryptionKey(TestSpaces.ENCRYPTION_KEY), SALT);
    private static final TextEncryptor LEGACY_WRITER = LegacyKeys.writer(TestSpaces.ENCRYPTION_KEY, SALT);
    private static final SpaceSealers SEALERS = space -> SpaceSealer.of(CURRENT);
    private static final LegacySpaceOpeners LEGACY = space -> LegacySpaceOpener.of(DataKeys.legacy(new EncryptionKey(TestSpaces.ENCRYPTION_KEY), SALT));
    private static final SealedColumn LABEL = SealedColumn.ofSpace("sealing_probes", "label_encrypted").withClearColumn("label");
    private static final SealedColumn NOTE = SealedColumn.ofSpace("sealing_probes", "note_encrypted");

    @Autowired JdbcClient jdbc;
    @Autowired SealedValueMigration migration;
    @Autowired SealingLock lock;

    @BeforeEach
    void scratchTable() {
        drop();
        jdbc.sql("""
            CREATE TABLE sealing_probes (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), space_id UUID NOT NULL,
              label TEXT, label_encrypted TEXT, note_encrypted TEXT)""").update();
        jdbc.sql("CREATE INDEX ON sealing_probes (space_id)").update();
    }

    @AfterEach
    void drop() {
        jdbc.sql("DROP TABLE IF EXISTS sealing_probes").update();
    }

    private UUID row(String label, String labelEncrypted, String noteEncrypted) {
        return jdbc.sql("INSERT INTO sealing_probes (space_id, label, label_encrypted, note_encrypted) VALUES (:s, :l, :le, :ne) RETURNING id")
            .param("s", A).param("l", label).param("le", labelEncrypted).param("ne", noteEncrypted)
            .query(UUID.class).single();
    }

    private Map<String, Object> read(UUID id) {
        return jdbc.sql("SELECT * FROM sealing_probes WHERE id = :id").param("id", id).query().singleRow();
    }

    @Test
    void a_value_in_clear_is_sealed_and_its_clear_column_emptied() {
        UUID id = row("Crème fraîche 🥛", null, null);

        assertThat(migration.migrate(LABEL, SEALERS, LEGACY)).isEqualTo(1);

        assertThat(read(id).get("label")).isNull();
        assertThat(SpaceSealer.of(CURRENT).open(LABEL, id, (String) read(id).get("label_encrypted"))).isEqualTo("Crème fraîche 🥛");
    }

    @Test
    void a_value_of_the_format_before_is_sealed_in_place() {
        UUID id = row(null, null, LEGACY_WRITER.encrypt("Loyer"));

        assertThat(migration.pending(NOTE)).isTrue();
        migration.migrate(NOTE, SEALERS, LEGACY);

        assertThat(migration.pending(NOTE)).isFalse();
        assertThat(SpaceSealer.of(CURRENT).open(NOTE, id, (String) read(id).get("note_encrypted"))).isEqualTo("Loyer");
    }

    @Test
    void a_clear_column_068_already_dropped_is_not_looked_for() {
        UUID id = row(null, null, LEGACY_WRITER.encrypt("Loyer"));
        SealedColumn droppedClear = SealedColumn.ofSpace("sealing_probes", "note_encrypted").withClearColumn("note");

        assertThat(migration.pending(droppedClear)).isTrue();
        migration.migrate(droppedClear, SEALERS, LEGACY);

        assertThat(SpaceSealer.of(CURRENT).open(droppedClear, id, (String) read(id).get("note_encrypted"))).isEqualTo("Loyer");
    }

    @Test
    void the_clear_value_wins_over_an_older_ciphertext_and_a_sealed_value_is_left_alone() {
        UUID clearWins = row("Nouveau", LEGACY_WRITER.encrypt("Ancien"), null);
        UUID sealed = jdbc.sql("SELECT gen_random_uuid()").query(UUID.class).single();
        String alreadySealed = SpaceSealer.of(CURRENT).seal(LABEL, sealed, "Déjà");
        jdbc.sql("INSERT INTO sealing_probes (id, space_id, label_encrypted) VALUES (:id, :s, :v)")
            .param("id", sealed).param("s", A).param("v", alreadySealed).update();

        assertThat(migration.migrate(LABEL, SEALERS, LEGACY)).isEqualTo(1);

        assertThat(SpaceSealer.of(CURRENT).open(LABEL, clearWins, (String) read(clearWins).get("label_encrypted"))).isEqualTo("Nouveau");
        assertThat(read(sealed).get("label_encrypted")).isEqualTo(alreadySealed);
    }

    @Test
    void a_sealing_that_would_not_give_the_value_back_stops_before_writing_anything() {
        UUID id = row("Pâtes", null, null);
        TextEncryptor lying = new TextEncryptor() {
            @Override public String encrypt(String text) { return CURRENT.encrypt(text); }
            @Override public String decrypt(String encrypted) { return CURRENT.decrypt(CURRENT.encrypt("not what was sealed")); }
        };

        assertThatThrownBy(() -> migration.migrate(LABEL, space -> SpaceSealer.of(lying), LEGACY))
            .hasMessageContaining("would not give its value back")
            .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain("Pâtes"));
        assertThat(read(id).get("label")).isEqualTo("Pâtes");
        assertThat(read(id).get("label_encrypted")).isNull();
    }

    @Test
    void a_row_that_cannot_be_sealed_stops_the_run_and_never_shows_its_value() {
        UUID secret = row("Secret de famille", null, null);

        assertThatThrownBy(() -> migration.migrate(LABEL, space -> {
                throw new IllegalArgumentException("Detected a Non-hex character");
            }, LEGACY))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Could not seal row " + secret + " of sealing_probes.label_encrypted")
            .hasNoCause()
            .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain("Secret de famille"));
        assertThat(read(secret).get("label")).isEqualTo("Secret de famille");
    }

    @Test
    void a_second_run_finds_nothing_left_and_changes_nothing() {
        UUID id = row("Pâtes", null, null);
        migration.migrate(LABEL, SEALERS, LEGACY);
        Object sealed = read(id).get("label_encrypted");

        assertThat(migration.pending(LABEL)).isFalse();
        assertThat(migration.migrate(LABEL, SEALERS, LEGACY)).isZero();
        assertThat(read(id).get("label_encrypted")).isEqualTo(sealed);
    }

    @Test
    void rows_that_refuse_to_change_stop_the_run_instead_of_looping() {
        row("Pâtes", null, null);
        jdbc.sql("CREATE FUNCTION sealing_probes_frozen() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$").update();
        jdbc.sql("CREATE TRIGGER frozen BEFORE UPDATE ON sealing_probes FOR EACH ROW EXECUTE FUNCTION sealing_probes_frozen()").update();
        try {
            assertThatThrownBy(() -> migration.migrate(LABEL, SEALERS, LEGACY)).hasMessageContaining("stay unsealed");
        } finally {
            jdbc.sql("DROP TABLE sealing_probes").update();
            jdbc.sql("DROP FUNCTION sealing_probes_frozen()").update();
        }
    }

    @Test
    void two_migrations_at_once_both_succeed_under_the_lock() throws Exception {
        jdbc.sql("INSERT INTO sealing_probes (space_id, label) SELECT :s, 'Article ' || n FROM generate_series(1, 3000) n")
            .param("s", A).update();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> runs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            runs.add(pool.submit(() -> {
                go.await();
                lock.whileHeld(() -> migration.migrate(LABEL, SEALERS, LEGACY));
                return null;
            }));
        }
        go.countDown();
        for (Future<?> run : runs) {
            run.get();
        }
        pool.shutdown();

        assertThat(migration.pending(LABEL)).isFalse();
    }

    @Test
    void a_value_sealed_with_the_legacy_key_is_sealed_again_with_the_current_one() {
        UUID id = jdbc.sql("SELECT gen_random_uuid()").query(UUID.class).single();
        jdbc.sql("INSERT INTO sealing_probes (id, space_id, note_encrypted) VALUES (:id, :s, :v)")
            .param("id", id).param("s", A).param("v", LegacyKeys.sealedV2(TestSpaces.ENCRYPTION_KEY, SALT, NOTE, id, "Loyer")).update();

        assertThat(migration.pending(NOTE)).isTrue();
        assertThat(migration.migrate(NOTE, SEALERS, LEGACY)).isEqualTo(1);

        String stored = (String) read(id).get("note_encrypted");
        assertThat(stored).startsWith("v3:");
        assertThat(SpaceSealer.of(CURRENT).open(NOTE, id, stored)).isEqualTo("Loyer");
        assertThat(migration.pending(NOTE)).isFalse();
    }

    @Test
    void formats_mixed_in_one_column_all_end_up_current() {
        UUID v1 = row(null, null, LEGACY_WRITER.encrypt("Ancien"));
        UUID v2 = jdbc.sql("SELECT gen_random_uuid()").query(UUID.class).single();
        jdbc.sql("INSERT INTO sealing_probes (id, space_id, note_encrypted) VALUES (:id, :s, :v)")
            .param("id", v2).param("s", A).param("v", LegacyKeys.sealedV2(TestSpaces.ENCRYPTION_KEY, SALT, NOTE, v2, "Scellé")).update();
        UUID v3 = jdbc.sql("SELECT gen_random_uuid()").query(UUID.class).single();
        String current = SpaceSealer.of(CURRENT).seal(NOTE, v3, "Courant");
        jdbc.sql("INSERT INTO sealing_probes (id, space_id, note_encrypted) VALUES (:id, :s, :v)")
            .param("id", v3).param("s", A).param("v", current).update();

        assertThat(migration.migrate(NOTE, SEALERS, LEGACY)).isEqualTo(2);

        assertThat(SpaceSealer.of(CURRENT).open(NOTE, v1, (String) read(v1).get("note_encrypted"))).isEqualTo("Ancien");
        assertThat(SpaceSealer.of(CURRENT).open(NOTE, v2, (String) read(v2).get("note_encrypted"))).isEqualTo("Scellé");
        assertThat(read(v3).get("note_encrypted")).isEqualTo(current);
    }

    @Test
    void a_legacy_value_moved_from_another_row_stops_the_run_and_never_shows_its_value() {
        UUID moved = jdbc.sql("SELECT gen_random_uuid()").query(UUID.class).single();
        String elsewhere = LegacyKeys.sealedV2(TestSpaces.ENCRYPTION_KEY, SALT, NOTE, UUID.randomUUID(), "Secret de famille");
        jdbc.sql("INSERT INTO sealing_probes (id, space_id, note_encrypted) VALUES (:id, :s, :v)")
            .param("id", moved).param("s", A).param("v", elsewhere).update();

        assertThatThrownBy(() -> migration.migrate(NOTE, SEALERS, LEGACY))
            .hasMessageContaining("Could not seal row " + moved + " of sealing_probes.note_encrypted")
            .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain("Secret de famille"));
        assertThat(read(moved).get("note_encrypted")).isEqualTo(elsewhere);
    }
}
