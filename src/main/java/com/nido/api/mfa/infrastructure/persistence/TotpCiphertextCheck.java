package com.nido.api.mfa.infrastructure.persistence;

import com.nido.api.infrastructure.sealing.ExistingCiphertextCheck;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Two-factor secrets are encrypted with the master key too, under a key per user rather than per space: on an
 * installation whose only encrypted data they are, they are what proves the key — see ExistingCiphertextCheck. As for
 * the spaces, one secret that opens is enough: a single damaged one is not taken for a wrong key.
 */
@Component
public class TotpCiphertextCheck implements ExistingCiphertextCheck {

    /** Secrets tried: enough that a few damaged ones cannot pass for a wrong key. */
    static final int SAMPLES = 5;

    private final JdbcClient jdbc;
    private final TotpEncryptorFactory encryptors;

    public TotpCiphertextCheck(JdbcClient jdbc, TotpEncryptorFactory encryptors) {
        this.jdbc = jdbc;
        this.encryptors = encryptors;
    }

    @Override
    public void verify() {
        List<Map.Entry<UUID, String>> samples = jdbc.sql("SELECT user_id, secret FROM two_factor_methods "
                + "WHERE method = 'APP' ORDER BY user_id LIMIT " + SAMPLES)
            .query((rs, rowNum) -> Map.entry(rs.getObject("user_id", UUID.class), rs.getString("secret")))
            .list();
        if (!samples.isEmpty() && samples.stream().noneMatch(this::opens)) {
            throw new IllegalStateException("The encryption key does not decrypt the two-factor secrets already "
                + "encrypted: nothing was encrypted with it. Start with the key this database was encrypted with.");
        }
    }

    private boolean opens(Map.Entry<UUID, String> sample) {
        try {
            encryptors.forUser(sample.getKey()).decrypt(sample.getValue());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
