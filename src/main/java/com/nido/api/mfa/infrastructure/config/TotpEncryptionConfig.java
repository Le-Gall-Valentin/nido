package com.nido.api.mfa.infrastructure.config;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nido.api.infrastructure.config.EncryptorCache;
import com.nido.api.infrastructure.config.ExistingCiphertextCheck;
import com.nido.api.shared.security.EncryptionKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Map;
import java.util.UUID;

@Configuration
public class TotpEncryptionConfig {

    // Per-user salt derived from the user UUID (32 hex chars). Security comes from the master secret
    // (PBKDF2 key stretching) combined with this per-user salt. Changing the master secret requires
    // re-enrollment (see key rotation procedure below).
    //
    // KEY ROTATION PROCEDURE: if the encryption key must be changed:
    //   1. Disable TOTP for all users (UPDATE users SET totp_secret=NULL, totp_enabled=FALSE)
    //   2. Deploy with the new key
    //   3. Users re-enroll at next login
    // There is no in-place re-encryption path because the old ciphertext requires the old key.
    @Bean
    TotpEncryptorFactory totpEncryptorFactory(EncryptionKey encryptionKey) {
        // Bounded and expiring — see EncryptorCache for why a derived key must not live forever.
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(id ->
            Encryptors.delux(encryptionKey.value(), id.toString().replace("-", "")));
        return cache::get;
    }

    /**
     * Two-factor secrets are encrypted with the master key too: on an installation whose only encrypted data
     * they are, one of them is what proves the key — see ExistingCiphertextCheck.
     */
    @Bean
    ExistingCiphertextCheck totpCiphertextCheck(JdbcClient jdbc, TotpEncryptorFactory encryptors) {
        return () -> jdbc.sql("SELECT user_id, totp_secret FROM user_totp WHERE totp_secret IS NOT NULL LIMIT 1")
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
