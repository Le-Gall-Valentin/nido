package com.synapsys.api.mfa.infrastructure.config;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class TotpEncryptionConfig {

    // Per-user salt derived from the user UUID (32 hex chars). Security comes from the master secret
    // (PBKDF2 key stretching) combined with this per-user salt. Changing the master secret requires
    // re-enrollment (see key rotation procedure below).
    //
    // KEY ROTATION PROCEDURE: if SYNAPSYS_ENCRYPTION_SECRET must be changed:
    //   1. Disable TOTP for all users (UPDATE users SET totp_secret=NULL, totp_enabled=FALSE)
    //   2. Deploy with the new key
    //   3. Users re-enroll at next login
    // There is no in-place re-encryption path because the old ciphertext requires the old key.
    @Bean
    TotpEncryptorFactory totpEncryptorFactory(SynapsysProperties properties) {
        ConcurrentHashMap<UUID, TextEncryptor> cache = new ConcurrentHashMap<>();
        return userId -> cache.computeIfAbsent(userId, id ->
            Encryptors.delux(properties.encryption().secret(), id.toString().replace("-", "")));
    }
}