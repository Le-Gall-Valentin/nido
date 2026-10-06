package com.nido.api.finance.infrastructure.config;

import com.nido.api.infrastructure.config.SpaceEncryptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FinanceEncryptionConfig {

    // One key per space, shared with every module that encrypts a space's data — see SpaceKeyCache.
    @Bean
    FinanceEncryptorFactory financeEncryptorFactory(SpaceEncryptorFactory spaces) {
        return spaces::forSpace;
    }
}
