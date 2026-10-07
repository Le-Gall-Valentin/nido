package com.nido.api.instance.infrastructure.persistence;

import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.KeyFingerprint;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/** The single row of {@code instance}, through plain SQL: one row, a few statements, nothing for JPA to add. */
@Component
public class InstanceStateAdapter implements InstanceStatePort {

    private final JdbcClient jdbc;

    public InstanceStateAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public InstanceState load() {
        return jdbc.sql("""
                SELECT setup_completed_at, key_fingerprint, key_fingerprint_salt, key_fingerprint_confirmed, key_generated
                FROM instance WHERE id = 1
                """)
            .query((rs, rowNum) -> {
                byte[] hash = rs.getBytes("key_fingerprint");
                byte[] salt = rs.getBytes("key_fingerprint_salt");
                return new InstanceState(
                    hash == null ? Optional.empty() : Optional.of(new KeyFingerprint(hash, salt)),
                    rs.getBoolean("key_fingerprint_confirmed"),
                    rs.getBoolean("key_generated"),
                    rs.getObject("setup_completed_at", OffsetDateTime.class) != null);
            })
            .single();
    }

    @Override
    public void recordFingerprint(KeyFingerprint fingerprint, boolean generated) {
        jdbc.sql("""
                UPDATE instance SET key_fingerprint = :hash, key_fingerprint_salt = :salt, key_fingerprint_confirmed = false,
                                    key_generated = :generated
                WHERE id = 1
                """)
            .param("hash", fingerprint.hash())
            .param("salt", fingerprint.salt())
            .param("generated", generated)
            .update();
    }

    @Override
    public boolean confirmFingerprint(KeyFingerprint fingerprint) {
        return jdbc.sql("""
                UPDATE instance SET key_fingerprint_confirmed = true
                WHERE id = 1 AND key_fingerprint = :hash AND NOT key_fingerprint_confirmed
                """)
            .param("hash", fingerprint.hash())
            .update() == 1;
    }

    @Override
    public boolean forgetFingerprint(KeyFingerprint fingerprint) {
        return jdbc.sql("""
                UPDATE instance SET key_fingerprint = NULL, key_fingerprint_salt = NULL, key_fingerprint_confirmed = true,
                                    key_generated = false
                WHERE id = 1 AND key_fingerprint = :hash AND NOT key_fingerprint_confirmed
                """)
            .param("hash", fingerprint.hash())
            .update() == 1;
    }

    @Override
    public boolean markSetupCompleted(Instant at) {
        return jdbc.sql("UPDATE instance SET setup_completed_at = :at WHERE id = 1 AND setup_completed_at IS NULL")
            .param("at", OffsetDateTime.ofInstant(at, ZoneOffset.UTC))
            .update() == 1;
    }
}
