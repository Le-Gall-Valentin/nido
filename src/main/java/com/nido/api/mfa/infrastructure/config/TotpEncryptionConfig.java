package com.nido.api.mfa.infrastructure.config;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nido.api.infrastructure.config.EncryptorCache;
import com.nido.api.infrastructure.config.NidoProperties;
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
    // KEY ROTATION PROCEDURE: if NIDO_ENCRYPTION_SECRET must be changed:
    //   1. Disable TOTP for all users (UPDATE users SET totp_secret=NULL, totp_enabled=FALSE)
    //   2. Deploy with the new key
    //   3. Users re-enroll at next login
    // There is no in-place re-encryption path because the old ciphertext requires the old key.
    @Bean
    TotpEncryptorFactory totpEncryptorFactory(NidoProperties properties) {
        // Bounded and expiring — see EncryptorCache for why a derived key must not live forever.
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(id ->
            Encryptors.delux(properties.encryption().secret(), id.toString().replace("-", "")));
        return cache::get;
    }
}