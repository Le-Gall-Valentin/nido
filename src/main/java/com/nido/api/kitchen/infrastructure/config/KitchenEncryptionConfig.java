package com.nido.api.kitchen.infrastructure.config;

import com.nido.api.infrastructure.config.EncryptionBackfill;
import com.nido.api.infrastructure.config.PlaintextTable;
import com.nido.api.infrastructure.config.PlaintextTableEncryptor;
import com.nido.api.infrastructure.config.PlaintextTablesBackfill;
import com.nido.api.infrastructure.config.SpaceEncryptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class KitchenEncryptionConfig {

    /** What versions before 0.13.1 stored in clear — see EncryptionBackfillRunner. */
    @Bean
    EncryptionBackfill kitchenEncryptionBackfill(PlaintextTableEncryptor encryptor, SpaceEncryptorFactory spaces) {
        return new PlaintextTablesBackfill(encryptor, spaces::forSpace,
            PlaintextTable.ofSpace("kitchen_recipes", "name", "description", "note"),
            PlaintextTable.throughParent("kitchen_recipe_ingredients", "recipe_id", "kitchen_recipes", "name"),
            PlaintextTable.throughParent("kitchen_recipe_steps", "recipe_id", "kitchen_recipes", "text"));
    }
}
