package com.nido.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** The schema of 0.13.1, on the migrated database every test runs against. */
@IntegrationTestConfig
class EncryptedColumnsIT {

    @Autowired JdbcTemplate jdbc;

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
}
