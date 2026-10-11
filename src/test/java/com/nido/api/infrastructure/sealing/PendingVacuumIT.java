package com.nido.api.infrastructure.sealing;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@IntegrationTestConfig
class PendingVacuumIT {

    @Autowired PendingVacuum pendingVacuum;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void scratchTables() {
        clear();
        jdbc.sql("CREATE TABLE pending_vacuum_probes (id INT PRIMARY KEY)").update();
        jdbc.sql("CREATE TABLE pending_vacuum_others (id INT PRIMARY KEY)").update();
    }

    @AfterEach
    void clear() {
        jdbc.sql("DELETE FROM sealing_vacuum_owed").update();
        jdbc.sql("DROP TABLE IF EXISTS pending_vacuum_probes").update();
        jdbc.sql("DROP TABLE IF EXISTS pending_vacuum_others").update();
    }

    @Test
    void owed_table_by_table_then_settled_table_by_table() {
        assertThat(pendingVacuum.anyOwed()).isFalse();

        pendingVacuum.owe(List.of("pending_vacuum_probes", "pending_vacuum_others"));
        assertThat(pendingVacuum.anyOwed()).isTrue();
        assertThat(pendingVacuum.stillOwed()).containsExactly("pending_vacuum_others", "pending_vacuum_probes");

        pendingVacuum.settle("pending_vacuum_probes");
        assertThat(pendingVacuum.stillOwed()).containsExactly("pending_vacuum_others");
    }

    @Test
    void a_table_rewritten_since_its_debt_owes_nothing_any_more() {
        pendingVacuum.owe(List.of("pending_vacuum_probes", "pending_vacuum_others"));

        // The command the warning gives, run by hand.
        jdbc.sql("VACUUM FULL pending_vacuum_probes").update();

        assertThat(pendingVacuum.stillOwed()).containsExactly("pending_vacuum_others");
    }

    @Test
    void a_table_owed_already_keeps_the_file_it_was_owed_for() {
        pendingVacuum.owe(List.of("pending_vacuum_probes"));
        jdbc.sql("VACUUM FULL pending_vacuum_probes").update();

        // A later start owes it again before the earlier debt was looked at: the rewrite in between still counts.
        pendingVacuum.owe(List.of("pending_vacuum_probes"));

        assertThat(pendingVacuum.stillOwed()).isEmpty();
    }

    @Test
    void a_table_gone_owes_nothing() {
        pendingVacuum.owe(List.of("pending_vacuum_probes"));
        jdbc.sql("DROP TABLE pending_vacuum_probes").update();

        assertThat(pendingVacuum.stillOwed()).isEmpty();
    }

    @Test
    void what_is_owed_is_vacuumed_and_settled_table_by_table_a_table_the_vacuum_could_not_take_staying_owed() {
        TableVacuum vacuum = mock(TableVacuum.class);
        when(vacuum.vacuumFull("pending_vacuum_others")).thenReturn(false);
        when(vacuum.vacuumFull("pending_vacuum_probes")).thenReturn(true);
        PendingVacuum vacuums = new PendingVacuum(jdbc, vacuum);
        vacuums.owe(List.of("pending_vacuum_probes", "pending_vacuum_others"));

        vacuums.vacuumOwed();

        verify(vacuum).vacuumFull("pending_vacuum_others");
        verify(vacuum).vacuumFull("pending_vacuum_probes");
        assertThat(vacuums.stillOwed()).containsExactly("pending_vacuum_others");
    }
}
