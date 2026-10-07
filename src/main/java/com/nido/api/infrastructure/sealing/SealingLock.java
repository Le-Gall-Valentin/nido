package com.nido.api.infrastructure.sealing;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Statement;

/**
 * One migration at a time across processes sharing the database: a Postgres session lock held, on a connection of its
 * own, for the length of the work. A second instance starting at the same moment waits, then finds nothing left —
 * where, without it, it read the rows the first was rewriting and failed on them.
 */
@Component
public class SealingLock {

    private static final String KEY = "hashtext('nido-sealed-values')";

    private final JdbcTemplate jdbc;

    public SealingLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void whileHeld(Runnable work) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT pg_advisory_lock(" + KEY + ")");
                try {
                    work.run();
                } finally {
                    statement.execute("SELECT pg_advisory_unlock(" + KEY + ")");
                }
            }
            return null;
        });
    }
}
