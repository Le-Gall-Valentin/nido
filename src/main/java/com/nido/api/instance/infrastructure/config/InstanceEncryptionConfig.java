package com.nido.api.instance.infrastructure.config;

import com.nido.api.infrastructure.sealing.RekeyedColumns;
import com.nido.api.instance.infrastructure.persistence.SettingsStoreAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class InstanceEncryptionConfig {

    /** The SMTP password, brought to the current key at start — see RekeyMigration. */
    @Bean
    RekeyedColumns instanceRekeyedColumns(SettingsStoreAdapter settings) {
        return RekeyedColumns.of(settings.rekeyedColumn());
    }
}
