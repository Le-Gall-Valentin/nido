package com.nido.api.calendar.infrastructure.config;

import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

@FunctionalInterface
public interface CalendarEncryptorFactory {
    TextEncryptor forSpace(UUID spaceId);
}
