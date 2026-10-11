package com.nido.api.instance.infrastructure.config;

import com.nido.api.infrastructure.sealing.LegacyFormats;
import com.nido.api.infrastructure.sealing.OwnKeyEncryptors;
import com.nido.api.infrastructure.sealing.RekeyedColumns;
import com.nido.api.instance.application.port.in.CloseLegacyFormatsUseCase;
import com.nido.api.instance.application.port.in.LegacyFormatsQuery;
import com.nido.api.instance.infrastructure.persistence.SettingsStoreAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration(proxyBeanMethods = false)
public class InstanceEncryptionConfig {

    /** The encryptor of the secret settings: one, for the store and for the migration. */
    @Bean(SettingsStoreAdapter.ENCRYPTOR)
    TextEncryptor settingsEncryptor(OwnKeyEncryptors encryptors) {
        return encryptors.forSalt(SettingsStoreAdapter.SALT);
    }

    /** The SMTP password, brought to the current key at start — see RekeyMigration. */
    @Bean
    RekeyedColumns instanceRekeyedColumns(@Qualifier(SettingsStoreAdapter.ENCRYPTOR) TextEncryptor encryptor) {
        return RekeyedColumns.of(SettingsStoreAdapter.rekeyedColumn(encryptor));
    }

    /**
     * Whether earlier formats are still taken — see RecordedLegacyFormats. No destroy method, whatever it comes to hold:
     * Spring takes a public close() or shutdown() of a bean for one, and would call it at every shutdown.
     */
    @Bean(destroyMethod = "")
    LegacyFormats legacyFormats(LegacyFormatsQuery query, CloseLegacyFormatsUseCase closing) {
        return new RecordedLegacyFormats(query, closing);
    }
}
