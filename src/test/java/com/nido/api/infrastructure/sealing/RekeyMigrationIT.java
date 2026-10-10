package com.nido.api.infrastructure.sealing;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.encryption.CurrentOrLegacyTextEncryptor;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class RekeyMigrationIT {

    private static final String SALT = "6e69646f2d6d61696c2d6f7574626f78";
    private static final CurrentOrLegacyTextEncryptor ENCRYPTOR =
        CurrentOrLegacyTextEncryptor.of(new EncryptionKey(TestSpaces.ENCRYPTION_KEY), SALT, () -> true);
    private static final TextEncryptor LEGACY_WRITER = LegacyKeys.writer(TestSpaces.ENCRYPTION_KEY, SALT);
    private static final RekeyedColumn VALUE = RekeyedColumn.of("rekey_probes", "value", "id", "kind = 'SECRET'", id -> ENCRYPTOR);

    @Autowired JdbcClient jdbc;
    @Autowired RekeyMigration migration;

    @BeforeEach
    void scratchTable() {
        drop();
        jdbc.sql("CREATE TABLE rekey_probes (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), kind TEXT NOT NULL, value TEXT)").update();
    }

    @AfterEach
    void drop() {
        jdbc.sql("DROP TABLE IF EXISTS rekey_probes").update();
    }

    private UUID row(String kind, String value) {
        return jdbc.sql("INSERT INTO rekey_probes (kind, value) VALUES (:k, :v) RETURNING id")
            .param("k", kind).param("v", value).query(UUID.class).single();
    }

    private String value(UUID id) {
        return jdbc.sql("SELECT value FROM rekey_probes WHERE id = :id").param("id", id).query(String.class).optional().orElse(null);
    }

    @Test
    void a_value_of_the_legacy_key_is_brought_to_the_current_one() {
        UUID id = row("SECRET", LEGACY_WRITER.encrypt("s3cret"));

        assertThat(migration.pending(VALUE)).isTrue();
        assertThat(migration.migrate(VALUE)).isEqualTo(1);

        assertThat(value(id)).startsWith("k2:");
        assertThat(ENCRYPTOR.decrypt(value(id))).isEqualTo("s3cret");
        assertThat(migration.pending(VALUE)).isFalse();
    }

    @Test
    void current_values_nulls_and_rows_outside_the_column_are_left_alone() {
        String current = ENCRYPTOR.encrypt("déjà");
        UUID done = row("SECRET", current);
        UUID empty = row("SECRET", null);
        UUID other = row("PLAIN", "smtp.example.com");

        assertThat(migration.pending(VALUE)).isFalse();
        assertThat(migration.migrate(VALUE)).isZero();

        assertThat(value(done)).isEqualTo(current);
        assertThat(value(empty)).isNull();
        assertThat(value(other)).isEqualTo("smtp.example.com");
    }

    @Test
    void an_unreadable_value_stops_the_run_names_its_row_and_never_shows_a_value() {
        UUID good = row("SECRET", LEGACY_WRITER.encrypt("Secret de famille"));
        UUID broken = row("SECRET", "not-a-ciphertext");

        assertThatThrownBy(() -> migration.migrate(VALUE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Could not re-encrypt row " + broken + " of rekey_probes.value")
            .hasNoCause()
            .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain("Secret de famille"));
        assertThat(value(broken)).isEqualTo("not-a-ciphertext");
        assertThat(value(good)).as("its batch was rolled back").doesNotStartWith("k2:");
    }

    @Test
    void a_column_that_deletes_unreadable_rows_drops_them_and_goes_on() {
        UUID good = row("SECRET", LEGACY_WRITER.encrypt("s3cret"));
        UUID broken = row("SECRET", "not-a-ciphertext");

        assertThat(migration.migrate(VALUE.deletingUnreadableRows())).isEqualTo(2);

        assertThat(value(broken)).isNull();
        assertThat(jdbc.sql("SELECT count(*) FROM rekey_probes WHERE id = :id").param("id", broken).query(Long.class).single()).isZero();
        assertThat(ENCRYPTOR.decrypt(value(good))).isEqualTo("s3cret");
    }

    @Test
    void a_rekeying_that_would_not_give_the_value_back_stops_before_writing_anything() {
        UUID id = row("SECRET", LEGACY_WRITER.encrypt("Pâtes"));
        TextEncryptor lying = new TextEncryptor() {
            @Override public String encrypt(String text) { return ENCRYPTOR.encrypt(text); }
            @Override public String decrypt(String stored) {
                return CurrentOrLegacyTextEncryptor.isCurrent(stored) ? "not what was written" : ENCRYPTOR.decrypt(stored);
            }
        };

        assertThatThrownBy(() -> migration.migrate(RekeyedColumn.of("rekey_probes", "value", "id", "kind = 'SECRET'", key -> lying)))
            .hasMessageContaining("would not give its value back")
            .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain("Pâtes"));
        assertThat(CurrentOrLegacyTextEncryptor.isCurrent(value(id))).isFalse();
    }

    @Test
    void rows_that_refuse_to_change_stop_the_run_instead_of_looping() {
        row("SECRET", LEGACY_WRITER.encrypt("s3cret"));
        jdbc.sql("CREATE FUNCTION rekey_probes_frozen() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$").update();
        jdbc.sql("CREATE TRIGGER frozen BEFORE UPDATE ON rekey_probes FOR EACH ROW EXECUTE FUNCTION rekey_probes_frozen()").update();
        try {
            assertThatThrownBy(() -> migration.migrate(VALUE)).hasMessageContaining("stay under the legacy key");
        } finally {
            jdbc.sql("DROP TABLE rekey_probes").update();
            jdbc.sql("DROP FUNCTION rekey_probes_frozen()").update();
        }
    }

    @Test
    void more_rows_than_a_batch_are_all_brought_to_the_current_key() {
        String legacy = LEGACY_WRITER.encrypt("s3cret");
        jdbc.sql("INSERT INTO rekey_probes (kind, value) SELECT 'SECRET', :v FROM generate_series(1, 1200)").param("v", legacy).update();

        assertThat(migration.migrate(VALUE)).isEqualTo(1200);
        assertThat(migration.pending(VALUE)).isFalse();
    }

    @Test
    // Without the check, the value is rewritten and read back as pending, batch after batch: a hang, not a failure.
    @Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void an_encryptor_that_would_write_without_the_current_prefix_stops_before_writing_anything() {
        UUID id = row("SECRET", LEGACY_WRITER.encrypt("s3cret"));
        // Reads like the real one, writes like versions up to 0.15.x: the value would stay pending for ever.
        TextEncryptor legacyWriting = new TextEncryptor() {
            @Override public String encrypt(String text) { return LEGACY_WRITER.encrypt(text); }
            @Override public String decrypt(String stored) { return ENCRYPTOR.decrypt(stored); }
        };

        assertThatThrownBy(() -> migration.migrate(RekeyedColumn.of("rekey_probes", "value", "id", "kind = 'SECRET'", key -> legacyWriting)))
            .hasMessageContaining("would not give its value back");
        assertThat(CurrentOrLegacyTextEncryptor.isCurrent(value(id))).isFalse();
    }

    @Test
    void a_value_changed_after_it_was_read_is_not_overwritten() {
        UUID changed = row("SECRET", LEGACY_WRITER.encrypt("ancien"));
        UUID other = row("SECRET", LEGACY_WRITER.encrypt("s3cret"));
        String newer = ENCRYPTOR.encrypt("nouveau");
        // Someone saves the setting while the migration holds the old value in hand.
        TextEncryptor racing = new TextEncryptor() {
            @Override public String encrypt(String text) { return ENCRYPTOR.encrypt(text); }
            @Override public String decrypt(String stored) {
                String value = ENCRYPTOR.decrypt(stored);
                if ("ancien".equals(value)) {
                    jdbc.sql("UPDATE rekey_probes SET value = :v WHERE id = :id").param("v", newer).param("id", changed).update();
                }
                return value;
            }
        };

        migration.migrate(RekeyedColumn.of("rekey_probes", "value", "id", "kind = 'SECRET'", key -> racing));

        assertThat(value(changed)).isEqualTo(newer);
        assertThat(ENCRYPTOR.decrypt(value(other))).isEqualTo("s3cret");
    }
}
