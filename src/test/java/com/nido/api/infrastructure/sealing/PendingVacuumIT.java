package com.nido.api.infrastructure.sealing;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class PendingVacuumIT {

    @Autowired PendingVacuum pendingVacuum;
    @Autowired JdbcClient jdbc;

    @AfterEach
    void putTheRowBack() {
        jdbc.sql("INSERT INTO sealing_state (id, vacuum_owed) VALUES (1, false) ON CONFLICT (id) DO UPDATE SET vacuum_owed = false")
            .update();
    }

    @Test
    void owed_then_settled() {
        pendingVacuum.owe();
        assertThat(pendingVacuum.isOwed()).isTrue();

        pendingVacuum.settle();
        assertThat(pendingVacuum.isOwed()).isFalse();
    }

    @Test
    void a_missing_row_counts_as_owed_and_comes_back_with_the_next_record() {
        // Unknown is not "nothing owed": vacuuming once too often costs a few seconds, never vacuuming leaves values.
        jdbc.sql("DELETE FROM sealing_state").update();

        assertThat(pendingVacuum.isOwed()).isTrue();
        pendingVacuum.settle();
        assertThat(pendingVacuum.isOwed()).isFalse();
    }
}
