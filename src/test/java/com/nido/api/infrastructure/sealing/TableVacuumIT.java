package com.nido.api.infrastructure.sealing;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.SharedContainers;
import com.nido.api.TestSpaces;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.encrypt.Encryptors;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The VACUUM run once values are sealed, against real Postgres, on a scratch table. */
@IntegrationTestConfig
class TableVacuumIT {

    private static final UUID A = UUID.randomUUID();
    private static final SpaceSealers SEALERS =
        space -> SpaceSealer.of(Encryptors.delux(TestSpaces.ENCRYPTION_KEY, "0123456789abcdef0123456789abcdef"));
    private static final SealedColumn LABEL = SealedColumn.ofSpace("vacuum_probes", "label_encrypted").withClearColumn("label");

    @Autowired JdbcClient jdbc;
    @Autowired TableVacuum vacuum;
    @Autowired SealedValueMigration migration;

    @BeforeEach
    void scratchTable() {
        drop();
        jdbc.sql("""
            CREATE TABLE vacuum_probes (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), space_id UUID NOT NULL,
              label VARCHAR(60), label_encrypted TEXT)""").update();
    }

    @AfterEach
    void drop() {
        jdbc.sql("DROP TABLE IF EXISTS vacuum_probes").update();
        jdbc.sql("DROP ROLE IF EXISTS vacuum_other_owner").update();
        jdbc.sql("DROP ROLE IF EXISTS vacuum_outsider").update();
    }

    private void probe(String label) {
        jdbc.sql("INSERT INTO vacuum_probes (space_id, label) VALUES (:s, :l)").param("s", A).param("l", label).update();
    }

    private long filenode() {
        return jdbc.sql("SELECT pg_relation_filenode('vacuum_probes')").query(Long.class).single();
    }

    @Test
    void vacuum_full_moves_the_table_to_new_files() {
        probe("Pâtes");
        long before = filenode();

        vacuum.vacuumFull("vacuum_probes");

        assertThat(filenode()).isNotEqualTo(before);
    }

    @Test
    void vacuum_also_refreshes_the_statistics_that_held_values_in_clear() {
        for (int i = 0; i < 20; i++) {
            probe("Secret de famille");
        }
        jdbc.sql("ANALYZE vacuum_probes").update();
        assertThat(statisticsOfLabel()).as("what autoanalyze keeps of the column").contains("Secret de famille");

        migration.migrate(LABEL, SEALERS);
        vacuum.vacuumFull("vacuum_probes");

        assertThat(statisticsOfLabel()).doesNotContain("Secret de famille");
    }

    @Test
    void a_table_owned_by_another_role_is_vacuumed_when_postgres_allows_it() {
        jdbc.sql("CREATE ROLE vacuum_other_owner").update();
        jdbc.sql("ALTER TABLE vacuum_probes OWNER TO vacuum_other_owner").update();
        long before = filenode();

        // The test user is a superuser: Postgres lets it vacuum a table it does not own.
        vacuum.vacuumFull("vacuum_probes");

        assertThat(filenode()).isNotEqualTo(before);
    }

    @Test
    void a_vacuum_that_cannot_run_warns_with_the_command_instead_of_stopping_the_start() {
        AtomicBoolean done = new AtomicBoolean(true);
        assertThat(warningsOf(() -> assertThatCode(() -> done.set(vacuum.vacuumFull("vacuum_absent"))).doesNotThrowAnyException()))
            .anySatisfy(line -> assertThat(line).contains("VACUUM (FULL, ANALYZE) vacuum_absent"));
        assertThat(done).as("not vacuumed").isFalse();
    }

    @Test
    void a_role_postgres_does_not_allow_gets_the_command_in_a_warning() {
        // Postgres does not fail a VACUUM it refuses: it skips the table with a warning of its own.
        jdbc.sql("CREATE ROLE vacuum_outsider LOGIN PASSWORD 'outsider'").update();
        TableVacuum asOutsider = new TableVacuum(new JdbcTemplate(
            new DriverManagerDataSource(SharedContainers.POSTGRES.getJdbcUrl(), "vacuum_outsider", "outsider")));
        long before = filenode();

        AtomicBoolean done = new AtomicBoolean(true);
        assertThat(warningsOf(() -> done.set(asOutsider.vacuumFull("vacuum_probes"))))
            .anySatisfy(line -> assertThat(line).contains("VACUUM (FULL, ANALYZE) vacuum_probes").contains("skipping"));
        assertThat(done).as("a table Postgres skipped is not vacuumed").isFalse();
        assertThat(filenode()).isEqualTo(before);
    }

    @Test
    void only_a_plain_identifier_is_vacuumed() {
        assertThatThrownBy(() -> vacuum.vacuumFull("vacuum_probes; DROP TABLE users"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<String> warningsOf(Runnable work) {
        Logger logger = (Logger) LoggerFactory.getLogger(TableVacuum.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        logger.addAppender(logged);
        try {
            work.run();
            return logged.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        } finally {
            logger.detachAppender(logged);
        }
    }

    private String statisticsOfLabel() {
        return jdbc.sql("""
                SELECT coalesce(most_common_vals::text, '') || coalesce(histogram_bounds::text, '')
                FROM pg_stats WHERE tablename = 'vacuum_probes' AND attname = 'label'""")
            .query(String.class).optional().orElse("");
    }
}
