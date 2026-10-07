package com.nido.api.infrastructure.sealing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * The VACUUM a start that seals values owes each table — see TableVacuum — kept in sealing_vacuum_owed (071) with the
 * file the table was in when the debt was recorded. Owed before the first row is rewritten, settled table by table once
 * vacuumed: a start stopped in between, after its last batch, leaves nothing to seal, and without this the next one
 * would never vacuum. A table whose file changed since was rewritten — by the VACUUM the warning gives to run by hand —
 * and owes nothing any more.
 */
@Component
public class PendingVacuum {

    private static final Logger log = LoggerFactory.getLogger(PendingVacuum.class);

    private final JdbcClient jdbc;

    public PendingVacuum(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean anyOwed() {
        return Boolean.TRUE.equals(jdbc.sql("SELECT EXISTS (SELECT 1 FROM sealing_vacuum_owed)").query(Boolean.class).single());
    }

    /** A table owed already keeps the file it was owed for. */
    public void owe(Collection<String> tables) {
        for (String table : tables) {
            jdbc.sql("""
                    INSERT INTO sealing_vacuum_owed (table_name, filenode)
                    VALUES (:table, pg_relation_filenode(to_regclass(:table))::bigint)
                    ON CONFLICT (table_name) DO NOTHING""")
                .param("table", SqlIdentifier.require(table))
                .update();
        }
    }

    /** The tables still owed, once those rewritten since their debt — or gone — are settled. */
    public List<String> stillOwed() {
        jdbc.sql("""
                DELETE FROM sealing_vacuum_owed
                WHERE pg_relation_filenode(to_regclass(table_name))::bigint IS DISTINCT FROM filenode
                RETURNING table_name""")
            .query(String.class)
            .list()
            .forEach(table -> log.info("The earlier versions of the values of {} are gone: the table was rewritten since", table));
        return jdbc.sql("SELECT table_name FROM sealing_vacuum_owed ORDER BY table_name").query(String.class).list();
    }

    public void settle(String table) {
        jdbc.sql("DELETE FROM sealing_vacuum_owed WHERE table_name = :table").param("table", table).update();
    }
}
