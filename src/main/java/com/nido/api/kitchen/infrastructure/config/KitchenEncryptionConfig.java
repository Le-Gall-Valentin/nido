package com.nido.api.kitchen.infrastructure.config;

import com.nido.api.infrastructure.sealing.SealedColumns;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeEntity;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeIngredientEntity;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeStepEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class KitchenEncryptionConfig {

    /** The columns of the kitchen that hold sealed values — see SealedValueMigration. */
    @Bean
    SealedColumns kitchenSealedColumns() {
        return SealedColumns.of(RecipeEntity.NAME, RecipeEntity.DESCRIPTION, RecipeEntity.NOTE, RecipeIngredientEntity.NAME,
            RecipeStepEntity.TEXT);
    }
}
