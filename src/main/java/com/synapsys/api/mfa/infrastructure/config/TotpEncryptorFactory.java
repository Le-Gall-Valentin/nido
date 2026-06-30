package com.synapsys.api.mfa.infrastructure.config;

import org.springframework.security.crypto.encrypt.TextEncryptor;
import java.util.UUID;

@FunctionalInterface
public interface TotpEncryptorFactory {
    TextEncryptor forUser(UUID userId);
}