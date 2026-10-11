package com.nido.api.mail.infrastructure.config;

import com.nido.api.infrastructure.sealing.OwnKeyEncryptors;
import com.nido.api.infrastructure.sealing.RekeyedColumns;
import com.nido.api.mail.infrastructure.persistence.MailOutboxAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration(proxyBeanMethods = false)
public class MailEncryptionConfig {

    /** The encryptor of the queue: one, for the outbox and for the migration. */
    @Bean(MailOutboxAdapter.ENCRYPTOR)
    TextEncryptor mailOutboxEncryptor(OwnKeyEncryptors encryptors) {
        return encryptors.forSalt(MailOutboxAdapter.SALT);
    }

    /** The mail queue, brought to the current key at start — see RekeyMigration. */
    @Bean
    RekeyedColumns mailRekeyedColumns(@Qualifier(MailOutboxAdapter.ENCRYPTOR) TextEncryptor encryptor) {
        return RekeyedColumns.of(MailOutboxAdapter.rekeyedColumn(encryptor));
    }
}
