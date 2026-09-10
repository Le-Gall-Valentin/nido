package com.nido.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema-wide invariants, checked against the real migrated database rather than against
 * the migrations that were supposed to produce it.
 *
 * <p>The counterpart to {@code ArchRulesTest} for the persistence layer: a rule here holds
 * for tables that do not exist yet, so the next one added inherits it instead of quietly
 * reintroducing a problem someone already fixed.
 */
@IntegrationTestConfig
class SchemaConventionsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired JdbcTemplate jdbcTemplate;

    /**
     * Every space-scoped table needs an index whose <em>leading</em> column is space_id.
     *
     * <p>Four tables were missing one, so listing a single household's recurring series,
     * savings goals or invitations scanned every household's. The rule matters more than the
     * four fixes: nothing about adding a space_id column suggests you also owe an index, and
     * the read paths that pay for it are nowhere near the migration that created the table.
     *
     * <p>Leading column, because that is what a lookup on space_id alone can use — a
     * (category_id, space_id) index would not serve it. Partial indexes are excluded for the
     * same reason: uq_space_invitations_pending leads with space_id but only covers
     * status = 'PENDING', so a listing that wants every invitation cannot use it.
     */
    @Test
    void every_table_with_a_space_id_column_has_an_index_leading_with_it() {
        List<String> unindexed = jdbcTemplate.queryForList("""
            SELECT c.table_name
            FROM information_schema.columns c
            WHERE c.table_schema = 'public'
              AND c.column_name = 'space_id'
              AND NOT EXISTS (
                SELECT 1
                FROM pg_index i
                JOIN pg_class t ON t.oid = i.indrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = i.indkey[0]
                WHERE n.nspname = 'public'
                  AND t.relname = c.table_name
                  AND a.attname = 'space_id'
                  AND i.indpred IS NULL
              )
            ORDER BY c.table_name
            """, String.class);

        assertThat(unindexed)
            .as("tables carrying space_id with no index leading on it — every space-scoped "
                + "read of these scans the whole table, across every space")
            .isEmpty();
    }

    /** Guards the query above: if it stopped seeing space-scoped tables at all, it would pass vacuously. */
    @Test
    void the_space_id_rule_is_actually_looking_at_something() {
        Integer spaceScopedTables = jdbcTemplate.queryForObject("""
            SELECT count(*) FROM information_schema.columns
            WHERE table_schema = 'public' AND column_name = 'space_id'
            """, Integer.class);

        assertThat(spaceScopedTables).isGreaterThanOrEqualTo(14);
    }
}
