package com.nido.api.space.infrastructure.config;

import com.nido.api.infrastructure.config.SpaceKeyCache;
import com.nido.api.infrastructure.encryption.DataKeys;
import com.nido.api.infrastructure.sealing.LegacySpaceOpener;
import com.nido.api.infrastructure.sealing.LegacySpaceOpeners;
import com.nido.api.infrastructure.sealing.SealedColumns;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.security.EncryptionKey;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SpaceEncryptionConfig {

    /**
     * The sealer of a space, for every module that keeps a space's data — the space module owns the salt.
     *
     * <p>Per-space salt read from spaces.encryption_salt — a random value drawn with the space, see SpaceSalt — and
     * not derived from the space UUID: what a space holds is personal ("RDV oncologue", a transaction label, a
     * shopping list), so its salt must not be guessable from an id that appears in every API URL. Changing the
     * master secret requires re-entering every value of every space, since a ciphertext needs the key it was written with.
     */
    @Bean
    SpaceSealers spaceSealers(SpaceKeyCache keys, GetSpaceEncryptionSaltUseCase salts) {
        return spaceId -> SpaceSealer.of(keys.forSpace(spaceId, () -> salts.getEncryptionSalt(spaceId)));
    }

    /**
     * The legacy opener of a space, for the migration and the key check only — see LegacySpaceOpener. Not cached: they
     * remember the openers for the length of a run (LegacySpaceOpeners.remembering).
     */
    @Bean
    LegacySpaceOpeners legacySpaceOpeners(EncryptionKey key, GetSpaceEncryptionSaltUseCase salts) {
        return spaceId -> LegacySpaceOpener.of(DataKeys.legacy(key, salts.getEncryptionSalt(spaceId)));
    }

    /** The columns of spaces that hold sealed values — see SealedValueMigration. */
    @Bean
    SealedColumns spaceSealedColumns() {
        return SealedColumns.of(SpaceEntity.NAME, SpaceEntity.DESCRIPTION);
    }
}
