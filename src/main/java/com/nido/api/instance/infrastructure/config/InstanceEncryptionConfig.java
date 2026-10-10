package com.nido.api.instance.infrastructure.config;

import com.nido.api.infrastructure.sealing.LegacyFormats;
import com.nido.api.infrastructure.sealing.RekeyedColumns;
import com.nido.api.instance.application.port.in.CloseLegacyFormatsUseCase;
import com.nido.api.instance.application.port.in.LegacyFormatsQuery;
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

    /**
     * Whether earlier formats are still taken, as the instance row records it. Asked at start and for each value without
     * the current prefix — rare once everything is converted; once closed they never open again, so that answer is kept.
     */
    @Bean
    LegacyFormats legacyFormats(LegacyFormatsQuery query, CloseLegacyFormatsUseCase closing) {
        return new LegacyFormats() {
            private volatile boolean closed;

            @Override
            public boolean closed() {
                if (!closed && query.closed()) {
                    closed = true;
                }
                return closed;
            }

            @Override
            public void closeForGood() {
                closing.closeForGood();
                closed = true;
            }
        };
    }
}
