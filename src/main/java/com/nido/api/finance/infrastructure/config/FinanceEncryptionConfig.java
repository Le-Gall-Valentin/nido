package com.nido.api.finance.infrastructure.config;

import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
        ConcurrentHashMap<UUID, TextEncryptor> cache = new ConcurrentHashMap<>();
        return spaceId -> cache.computeIfAbsent(spaceId, id ->
            Encryptors.delux(properties.encryption().secret(), getSpaceEncryptionSaltUseCase.getEncryptionSalt(id)));
    }
}
