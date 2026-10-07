package com.nido.api.mfa.infrastructure.persistence;

import com.nido.api.infrastructure.sealing.ExistingCiphertextCheck;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Two-factor secrets are encrypted with the master key too, under a key per user rather than per space: on an
 * installation whose only encrypted data they are, one of them is what proves the key — see ExistingCiphertextCheck.
 */
@Component
public class TotpCiphertextCheck implements ExistingCiphertextCheck {

    private final JdbcClient jdbc;
    private final TotpEncryptorFactory encryptors;

    public TotpCiphertextCheck(JdbcClient jdbc, TotpEncryptorFactory encryptors) {
        this.jdbc = jdbc;
        this.encryptors = encryptors;
    }

    @Override
    public void verify() {
        jdbc.sql("SELECT user_id, totp_secret FROM user_totp WHERE totp_secret IS NOT NULL LIMIT 1")
            .query((rs, rowNum) -> Map.entry(rs.getObject("user_id", UUID.class), rs.getString("totp_secret")))
            .optional()
            .ifPresent(sample -> {
                try {
                    encryptors.forUser(sample.getKey()).decrypt(sample.getValue());
                } catch (RuntimeException e) {
                    throw new IllegalStateException("The encryption key does not decrypt the two-factor secrets "
                        + "already encrypted: nothing was encrypted with it. Start with the key this database was "
                        + "encrypted with.");
                }
            });
    }
}
