package com.nido.api.mfa.infrastructure.config;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nido.api.infrastructure.config.EncryptorCache;
import com.nido.api.infrastructure.encryption.CurrentOrLegacyTextEncryptor;
import com.nido.api.infrastructure.sealing.RekeyedColumn;
import com.nido.api.infrastructure.sealing.RekeyedColumns;
import com.nido.api.shared.security.EncryptionKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

@Configuration
public class TotpEncryptionConfig {

    // Per-user salt derived from the user UUID (32 hex chars). Security comes from the master secret — the key is derived
    // from it and this salt, see DataKeys — so changing the master secret requires re-enrollment:
    //
    // KEY ROTATION PROCEDURE: if the encryption key must be changed:
    //   1. Remove every authenticator app (DELETE FROM two_factor_methods WHERE method = 'APP')
    //   2. Deploy with the new key — codes sent by mail and not used yet stop working too: their hashes are
    //      keyed by it (HmacMailCodeHasherAdapter); a new one is simply asked for
    //   3. Users turn the app on again at their next login
    // There is no in-place rotation of the master key: an old ciphertext requires the old master key.
    @Bean
    TotpEncryptorFactory totpEncryptorFactory(EncryptionKey encryptionKey) {
        // Bounded and expiring — see EncryptorCache for why a derived key must not live forever. Reads what versions up
        // to 0.15.x wrote too: an enrolment begun before the upgrade waits in Redis under the legacy key.
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(id ->
            CurrentOrLegacyTextEncryptor.of(encryptionKey, id.toString().replace("-", "")));
        return cache::get;
    }

    /** The authenticator secrets, brought to the current key at start — see RekeyMigration. */
    @Bean
    RekeyedColumns totpRekeyedColumns(TotpEncryptorFactory encryptors) {
        return RekeyedColumns.of(RekeyedColumn.of("two_factor_methods", "secret", "user_id", "method = 'APP'",
            userId -> encryptors.forUser(UUID.fromString(userId))));
    }
}
