package com.nido.api.shopping.infrastructure.config;

import com.nido.api.infrastructure.sealing.SealedColumns;
import com.nido.api.shopping.infrastructure.persistence.entity.ShoppingCategoryEntity;
import com.nido.api.shopping.infrastructure.persistence.entity.ShoppingItemEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ShoppingEncryptionConfig {

    /** The columns of shopping that hold sealed values — see SealedValueMigration. */
    @Bean
    SealedColumns shoppingSealedColumns() {
        return SealedColumns.of(ShoppingItemEntity.NAME, ShoppingCategoryEntity.NAME);
    }
}
