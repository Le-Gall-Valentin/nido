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
    private static final TextEncryptor KEY = Encryptors.delux(TestSpaces.ENCRYPTION_KEY, "0123456789abcdef0123456789abcdef");
    private static final SpaceSealers SEALERS = space -> SpaceSealer.of(KEY);
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

        assertThat(migration.migrate(LABEL, SEALERS)).isEqualTo(1);

        assertThat(read(id).get("label")).isNull();
        assertThat(SpaceSealer.of(KEY).open(LABEL, id, (String) read(id).get("label_encrypted"))).isEqualTo("Crème fraîche 🥛");
    }

    @Test
    void a_value_of_the_format_before_is_sealed_in_place() {
        UUID id = row(null, null, KEY.encrypt("Loyer"));

        assertThat(migration.pending(NOTE)).isTrue();
        migration.migrate(NOTE, SEALERS);

        assertThat(migration.pending(NOTE)).isFalse();
        assertThat(SpaceSealer.of(KEY).open(NOTE, id, (String) read(id).get("note_encrypted"))).isEqualTo("Loyer");
    }

    @Test
    void a_clear_column_068_already_dropped_is_not_looked_for() {
        UUID id = row(null, null, KEY.encrypt("Loyer"));
        SealedColumn droppedClear = SealedColumn.ofSpace("sealing_probes", "note_encrypted").withClearColumn("note");

        assertThat(migration.pending(droppedClear)).isTrue();
        migration.migrate(droppedClear, SEALERS);

        assertThat(SpaceSealer.of(KEY).open(droppedClear, id, (String) read(id).get("note_encrypted"))).isEqualTo("Loyer");
    }

    @Test
    void the_clear_value_wins_over_an_older_ciphertext_and_a_sealed_value_is_left_alone() {
        UUID clearWins = row("Nouveau", KEY.encrypt("Ancien"), null);
        UUID sealed = jdbc.sql("SELECT gen_random_uuid()").query(UUID.class).single();
        String alreadySealed = SpaceSealer.of(KEY).seal(LABEL, sealed, "Déjà");
        jdbc.sql("INSERT INTO sealing_probes (id, space_id, label_encrypted) VALUES (:id, :s, :v)")
            .param("id", sealed).param("s", A).param("v", alreadySealed).update();

        assertThat(migration.migrate(LABEL, SEALERS)).isEqualTo(1);

        assertThat(SpaceSealer.of(KEY).open(LABEL, clearWins, (String) read(clearWins).get("label_encrypted"))).isEqualTo("Nouveau");
        assertThat(read(sealed).get("label_encrypted")).isEqualTo(alreadySealed);
    }

    @Test
    void a_sealing_that_would_not_give_the_value_back_stops_before_writing_anything() {
        UUID id = row("Pâtes", null, null);
        TextEncryptor lying = new TextEncryptor() {
            @Override public String encrypt(String text) { return KEY.encrypt(text); }
            @Override public String decrypt(String encrypted) { return KEY.decrypt(KEY.encrypt("not what was sealed")); }
        };

        assertThatThrownBy(() -> migration.migrate(LABEL, space -> SpaceSealer.of(lying)))
            .hasMessageContaining("would not give its value back")
            .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain("Pâtes"));
        assertThat(read(id).get("label")).isEqualTo("Pâtes");
        assertThat(read(id).get("label_encrypted")).isNull();
    }

    @Test
    void rows_that_refuse_to_change_stop_the_run_instead_of_looping() {
        row("Pâtes", null, null);
        jdbc.sql("CREATE FUNCTION sealing_probes_frozen() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$").update();
        jdbc.sql("CREATE TRIGGER frozen BEFORE UPDATE ON sealing_probes FOR EACH ROW EXECUTE FUNCTION sealing_probes_frozen()").update();
        try {
            assertThatThrownBy(() -> migration.migrate(LABEL, SEALERS)).hasMessageContaining("stay unsealed");
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
                lock.whileHeld(() -> migration.migrate(LABEL, SEALERS));
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
}
