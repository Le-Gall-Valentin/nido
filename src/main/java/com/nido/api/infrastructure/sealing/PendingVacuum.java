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

    public boolean isOwed() {
        return jdbc.sql("SELECT vacuum_owed FROM sealing_state WHERE id = 1").query(Boolean.class).optional().orElse(false);
    }

    public void owe() {
        jdbc.sql("UPDATE sealing_state SET vacuum_owed = true WHERE id = 1").update();
    }

    public void settle() {
        jdbc.sql("UPDATE sealing_state SET vacuum_owed = false WHERE id = 1").update();
    }
}
