package com.nido.api.infrastructure.config;

import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

/** The encryptor of a space, by its id: the salt is asked of the space module when the key is not cached. */
@FunctionalInterface
public interface SpaceEncryptorFactory {
    TextEncryptor forSpace(UUID spaceId);
}
