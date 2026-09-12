package com.nido.api.finance.infrastructure.config;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nido.api.infrastructure.config.EncryptorCache;
import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

@Configuration
public class FinanceEncryptionConfig {

    // Per-space salt read from spaces.encryption_salt (a random value generated at space
    // creation — see SpaceEntity#generateEncryptionSaltIfMissing), not derived from the space
    // UUID itself: unlike TOTP's per-user salt, Finance data is sensitive enough that the
    // salt must not be guessable from an id already visible in every API URL.
    // Same key rotation limitation as TotpEncryptionConfig: changing the master secret
    // requires re-entering every Finance record, since old ciphertext needs the old key.
    @Bean
    FinanceEncryptorFactory financeEncryptorFactory(NidoProperties properties, GetSpaceEncryptionSaltUseCase getSpaceEncryptionSaltUseCase) {
        // Bounded and expiring — see EncryptorCache. Expiry is what makes the salt below re-read
        // periodically instead of being frozen at whatever it was when this instance booted.
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(id ->
            Encryptors.delux(properties.encryption().secret(), getSpaceEncryptionSaltUseCase.getEncryptionSalt(id)));
        return cache::get;
    }
}
