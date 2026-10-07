package com.nido.api.infrastructure.sealing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collection;

/**
 * Rewrites a table once its values in clear, or in the format before 0.14.0, are sealed. An UPDATE leaves the version it replaces in
 * the table's files, in clear, until something reuses the space — a plain VACUUM only marks it reusable.
 * VACUUM FULL writes the live rows to new files and removes the old ones; ANALYZE replaces the statistics
 * autoanalyze kept of the column, most common values included.
 *
 * <p>It locks the table exclusively, which is why it runs before the application serves anyone, and waits
 * at most {@link #LOCK_TIMEOUT} for that lock: a backup running at that moment holds the table for
 * minutes. Whatever stops it — that wait, a role Postgres does not allow, a full disk — is a warning with
 * the command to run, not a failed start: the data itself is sealed already.
 */
@Component
public class TableVacuum {

    static final String LOCK_TIMEOUT = "10s";

    private static final Logger log = LoggerFactory.getLogger(TableVacuum.class);

    private final JdbcTemplate jdbc;

    public TableVacuum(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return whether every table was vacuumed — those that were not got their warning */
    public boolean vacuumFull(Collection<String> tables) {
        boolean all = true;
        for (String table : tables) {
            SqlIdentifier.require(table);
            try {
                String skipped = jdbc.execute((ConnectionCallback<String>) connection -> {
                    // One connection for the three statements, outside any transaction, which VACUUM refuses:
                    // autocommit is forced rather than assumed of the pool, and given back as it was found.
                    boolean autoCommit = connection.getAutoCommit();
                    if (!autoCommit) {
                        connection.setAutoCommit(true);
                    }
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("SET lock_timeout = '" + LOCK_TIMEOUT + "'");
                        try {
                            statement.execute("VACUUM (FULL, ANALYZE) " + table);
                            SQLWarning warning = statement.getWarnings();
                            return warning == null ? null : warning.getMessage();
                        } finally {
                            statement.execute("RESET lock_timeout");
                        }
                    } finally {
                        if (!autoCommit) {
                            connection.setAutoCommit(false);
                        }
                    }
                });
                if (skipped != null) {
                    warnNotVacuumed(table, skipped);
                    all = false;
                }
            } catch (DataAccessException e) {
                warnNotVacuumed(table, e.getMostSpecificCause().getMessage());
                all = false;
            }
        }
        return all;
    }

    private static void warnNotVacuumed(String table, String why) {
        log.warn("The values of {} are sealed, but their earlier versions stay in its files ({}). "
            + "Run, as a role allowed to: VACUUM (FULL, ANALYZE) {};", table, why, table);
    }
}
