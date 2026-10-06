package com.nido.api.shopping.infrastructure.config;

import com.nido.api.infrastructure.config.EncryptionBackfill;
import com.nido.api.infrastructure.config.PlaintextTable;
import com.nido.api.infrastructure.config.PlaintextTableEncryptor;
import com.nido.api.infrastructure.config.PlaintextTablesBackfill;
import com.nido.api.infrastructure.config.SpaceEncryptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ShoppingEncryptionConfig {

    /** What versions before 0.13.1 stored in clear — see EncryptionBackfillRunner. */
    @Bean
    EncryptionBackfill shoppingEncryptionBackfill(PlaintextTableEncryptor encryptor, SpaceEncryptorFactory spaces) {
        return new PlaintextTablesBackfill(encryptor, spaces::forSpace,
            PlaintextTable.ofSpace("shopping_items", "name"),
            PlaintextTable.ofSpace("shopping_categories", "name"));
    }
}
