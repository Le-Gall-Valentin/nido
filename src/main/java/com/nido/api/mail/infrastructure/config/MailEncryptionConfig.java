package com.nido.api.mail.infrastructure.config;

import com.nido.api.infrastructure.sealing.RekeyedColumns;
import com.nido.api.mail.infrastructure.persistence.MailOutboxAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MailEncryptionConfig {

    /** The mail queue, brought to the current key at start — see RekeyMigration. */
    @Bean
    RekeyedColumns mailRekeyedColumns(MailOutboxAdapter outbox) {
        return RekeyedColumns.of(outbox.rekeyedColumn());
    }
}
