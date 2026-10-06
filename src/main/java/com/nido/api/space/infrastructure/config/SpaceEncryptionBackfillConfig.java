package com.nido.api.space.infrastructure.config;

import com.nido.api.infrastructure.config.EncryptionBackfill;
import com.nido.api.infrastructure.config.PlaintextTable;
import com.nido.api.infrastructure.config.PlaintextTableEncryptor;
import com.nido.api.infrastructure.config.PlaintextTablesBackfill;
import com.nido.api.infrastructure.config.SpaceEncryptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SpaceEncryptionBackfillConfig {

    /** The names and descriptions versions before 0.13.1 stored in clear — see EncryptionBackfillRunner. */
    @Bean
    EncryptionBackfill spaceEncryptionBackfill(PlaintextTableEncryptor encryptor, SpaceEncryptorFactory spaces) {
        return new PlaintextTablesBackfill(encryptor, spaces::forSpace,
            PlaintextTable.ofSpaceItself("spaces", "name", "description"));
    }
}
