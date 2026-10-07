package com.nido.api.space.infrastructure.config;

import com.nido.api.infrastructure.sealing.SealedColumns;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SpaceEncryptionBackfillConfig {

    /** The columns of spaces that hold sealed values — see SealedValueMigration. */
    @Bean
    SealedColumns spaceSealedColumns() {
        return SealedColumns.of(SpaceEntity.NAME, SpaceEntity.DESCRIPTION);
    }
}
