package com.nido.api.mfa.infrastructure.config;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nido.api.infrastructure.config.EncryptorCache;
import com.nido.api.shared.security.EncryptionKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

@Configuration
public class TotpEncryptionConfig {

    // Per-user salt derived from the user UUID (32 hex chars). Security comes from the master secret
    // (PBKDF2 key stretching) combined with this per-user salt. Changing the master secret requires
    // re-enrollment (see key rotation procedure below).
    //
    // KEY ROTATION PROCEDURE: if the encryption key must be changed:
    //   1. Remove every authenticator app (DELETE FROM two_factor_methods WHERE method = 'APP')
    //   2. Deploy with the new key — codes sent by mail and not used yet stop working too: their hashes are
    //      keyed by it (HmacMailCodeHasherAdapter); a new one is simply asked for
    //   3. Users turn the app on again at their next login
    // There is no in-place re-encryption path because the old ciphertext requires the old key.
    @Bean
    TotpEncryptorFactory totpEncryptorFactory(EncryptionKey encryptionKey) {
        // Bounded and expiring — see EncryptorCache for why a derived key must not live forever.
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(id ->
            Encryptors.delux(encryptionKey.value(), id.toString().replace("-", "")));
        return cache::get;
    }
}
