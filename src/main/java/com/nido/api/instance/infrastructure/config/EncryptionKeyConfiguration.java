package com.nido.api.instance.infrastructure.config;

import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.instance.application.port.in.ResolveEncryptionKeyUseCase;
import com.nido.api.shared.security.EncryptionKey;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration(proxyBeanMethods = false)
public class EncryptionKeyConfiguration {

    /**
     * Decided after Liquibase, which creates the table the fingerprint lives in — the SpringLiquibase
     * parameter is there to order the two, nothing else — and before every encryptor, which take this
     * bean. A refusal throws here and the application does not start.
     */
    @Bean
    EncryptionKey encryptionKey(SpringLiquibase liquibase, NidoProperties properties, ResolveEncryptionKeyUseCase resolve) {
        String configured = properties.encryption() == null ? null : properties.encryption().secret();
        return new EncryptionKey(resolve.resolve(configured));
    }
}
