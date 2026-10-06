package com.nido.api.infrastructure.config;

import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SpaceEncryptionConfig {

    // Per-space salt read from spaces.encryption_salt — a random value generated with the space, see
    // SpaceEntity — and not derived from the space UUID: what a space holds is personal ("RDV
    // oncologue", a transaction label, a shopping list), so its salt must not be guessable from an id
    // that appears in every API URL.
    // Key rotation limitation: changing the master secret requires re-entering every record of every
    // space, since old ciphertext needs the old key.
    @Bean
    SpaceEncryptorFactory spaceEncryptorFactory(SpaceKeyCache keys, GetSpaceEncryptionSaltUseCase salts) {
        return spaceId -> keys.forSpace(spaceId, () -> salts.getEncryptionSalt(spaceId));
    }
}
