package com.nido.api.finance.infrastructure.config;

import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

@FunctionalInterface
public interface FinanceEncryptorFactory {
    TextEncryptor forSpace(UUID spaceId);
}
