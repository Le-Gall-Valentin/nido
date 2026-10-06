package com.nido.api.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.nido.api.shared.security.EncryptionKey;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * The key of a space: {@code Encryptors.delux(master key, spaces.encryption_salt)}, derived once and kept
 * as long as {@link EncryptorCache} allows. Every module that encrypts a space's data uses it — finance,
 * calendar, shopping, tasks, kitchen and the space itself — so a space has one key, and the application
 * one cache of them.
 *
 * <p>Takes the salt from its caller instead of looking it up: the space module reads it in the very row
 * it decrypts, and asking the space module for it from here would loop back into the space module's own
 * repository. Every other module goes through {@link SpaceEncryptorFactory}.
 */
@Component
public class SpaceKeyCache {

    private final EncryptionKey encryptionKey;
    private final Cache<UUID, TextEncryptor> keys = EncryptorCache.buildWithoutLoader();

    public SpaceKeyCache(EncryptionKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    /** The salt is only read when the key is not cached already. */
    public TextEncryptor forSpace(UUID spaceId, Supplier<String> salt) {
        return keys.get(spaceId, id -> derive(salt.get()));
    }

    /**
     * For a space that has no id yet: its name is encrypted before its row is inserted. Not cached —
     * there is nothing to cache it under — and a space is created rarely enough for one derivation.
     */
    public TextEncryptor forNewSpace(String salt) {
        return derive(salt);
    }

    private TextEncryptor derive(String salt) {
        return Encryptors.delux(encryptionKey.value(), salt);
    }
}
