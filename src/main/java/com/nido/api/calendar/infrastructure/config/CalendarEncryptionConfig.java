package com.nido.api.calendar.infrastructure.config;

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
public class CalendarEncryptionConfig {

    // Per-space salt read from spaces.encryption_salt (a random value generated at space creation
    // — see SpaceEntity#generateEncryptionSaltIfMissing), not derived from the space UUID itself:
    // a calendar's contents are at least as personal as a transaction label ("RDV oncologue"),
    // so the salt must not be guessable from an id that appears in every API URL.
    // Same key rotation limitation as Finance and TOTP: changing the master secret requires
    // re-entering every calendar record, since old ciphertext needs the old key.
    @Bean
    CalendarEncryptorFactory calendarEncryptorFactory(
            NidoProperties properties, GetSpaceEncryptionSaltUseCase getSpaceEncryptionSaltUseCase) {
        // Bounded and expiring — see EncryptorCache. Expiry is what makes the salt below re-read
        // periodically instead of being frozen at whatever it was when this instance booted.
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(id ->
            Encryptors.delux(properties.encryption().secret(), getSpaceEncryptionSaltUseCase.getEncryptionSalt(id)));
        return cache::get;
    }
}
