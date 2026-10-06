package com.nido.api.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Rewrites a table once its values in clear are encrypted. An UPDATE leaves the version it replaces in
 * the table's files, in clear, until something reuses the space — a plain VACUUM only marks it reusable.
 * VACUUM FULL writes the live rows to new files and removes the old ones.
 *
 * <p>It locks the table exclusively, which is why it runs before the application serves anyone. Only the
 * table's owner may run it: anyone else gets the command in a warning, the data itself being encrypted
 * already.
 */
@Component
public class TableVacuum {

    private static final Logger log = LoggerFactory.getLogger(TableVacuum.class);
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z_]*");

    private final JdbcClient jdbc;

    public TableVacuum(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void vacuumFull(Collection<String> tables) {
        for (String table : tables) {
            if (!IDENTIFIER.matcher(table).matches()) {
                throw new IllegalArgumentException("Not a plain SQL identifier: " + table);
            }
            if (ownedByUs(table)) {
                // Outside any transaction, which VACUUM refuses: this connection is in autocommit.
                jdbc.sql("VACUUM FULL " + table).update();
            } else {
                log.warn("The values of {} are encrypted, but their earlier versions in clear stay in its files "
                    + "until its owner runs: VACUUM FULL {};", table, table);
            }
        }
    }

    private boolean ownedByUs(String table) {
        return Boolean.TRUE.equals(jdbc.sql(
                "SELECT pg_get_userbyid(relowner) = current_user FROM pg_class WHERE oid = to_regclass(:table)")
            .param("table", table)
            .query(Boolean.class).single());
    }
}
