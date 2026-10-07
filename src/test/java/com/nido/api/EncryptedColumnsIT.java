package com.nido.api;

import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.infrastructure.sealing.SealedColumns;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The schema of the encrypted columns, on the migrated database every test runs against. */
@IntegrationTestConfig
class EncryptedColumnsIT {

    @Autowired JdbcTemplate jdbc;
    @Autowired List<SealedColumns> declared;

    @Test
    void every_text_of_the_perimeter_has_an_encrypted_column_of_type_text() {
        assertThat(PlaintextPerimeter.encryptedColumnsMissingOrNotText(jdbc)).isEmpty();
    }

    @Test
    void a_default_finance_category_may_hold_an_encrypted_label() {
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM pg_constraint WHERE conname = 'chk_finance_categories_label_xor'", Long.class))
            .isZero();
    }

    @Test
    void no_column_of_the_perimeter_is_left_in_clear_and_the_required_ones_are_required() {
        assertThat(PlaintextPerimeter.columnsInClear(jdbc)).isEmpty();
        assertThat(PlaintextPerimeter.requiredEncryptedColumnsAcceptingNull(jdbc)).isEmpty();
    }

    @Test
    void every_encrypted_column_of_the_schema_is_declared_sealed() {
        // A column left out would keep the format before 0.14.0, and strict reading would refuse its whole table.
        List<String> declaredNames = declared.stream().flatMap(d -> d.columns().stream()).map(SealedColumn::toString).toList();
        List<String> inSchema = jdbc.queryForList("""
            SELECT table_name || '.' || column_name FROM information_schema.columns
            WHERE table_schema = current_schema() AND column_name LIKE '%\\_encrypted' ESCAPE '\\'""", String.class);

        assertThat(declaredNames).containsExactlyInAnyOrderElementsOf(inSchema);
    }
}
