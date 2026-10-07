package com.nido.api.infrastructure.sealing;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Whether a start that sealed values still owes the VACUUM that removes their earlier versions — see TableVacuum.
 * Owed before the first row is rewritten, settled once the VACUUM ran: a start stopped in between, after its last
 * batch, leaves nothing to seal, and without this the next one would never vacuum. Kept in sealing_state (069).
 */
@Component
public class PendingVacuum {

    private final JdbcClient jdbc;

    public PendingVacuum(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** A missing row counts as owed: vacuuming once too often costs seconds, never vacuuming leaves old values. */
    public boolean isOwed() {
        return jdbc.sql("SELECT vacuum_owed FROM sealing_state WHERE id = 1").query(Boolean.class).optional().orElse(true);
    }

    public void owe() {
        record(true);
    }

    public void settle() {
        record(false);
    }

    private void record(boolean owed) {
        jdbc.sql("""
                INSERT INTO sealing_state (id, vacuum_owed) VALUES (1, :owed)
                ON CONFLICT (id) DO UPDATE SET vacuum_owed = EXCLUDED.vacuum_owed""")
            .param("owed", owed)
            .update();
    }
}
