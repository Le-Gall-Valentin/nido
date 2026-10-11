package com.nido.api.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.nido.api.infrastructure.encryption.DataKeys;
import com.nido.api.shared.security.EncryptionKey;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * The key of a space: {@code DataKeys.current(master key, spaces.encryption_salt)}, derived once and kept
 * as long as {@link EncryptorCache} allows. Every module that encrypts a space's data uses it — finance,
 * calendar, shopping, tasks, kitchen and the space itself — so a space has one key, and the application
 * one cache of them.
 *
 * <p>Takes the salt from its caller instead of looking it up: the space module reads it in the very row
 * it opens, and asking the space module for it from here would loop back into the space module's own
 * repository. Every other module goes through {@link com.nido.api.infrastructure.sealing.SpaceSealers}.
 */
@Component
public class SpaceKeyCache {

    private final EncryptionKey encryptionKey;
    private final Cache<UUID, TextEncryptor> keys = EncryptorCache.buildWithoutLoader();

    public SpaceKeyCache(EncryptionKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    /**
     * The salt is only read when the key is not cached already. It is read inside the cache's computation, so
     * the supplier must never decrypt anything: the computation would call back into this cache.
     */
    public TextEncryptor forSpace(UUID spaceId, Supplier<String> salt) {
        return keys.get(spaceId, id -> derive(salt.get()));
    }

    private TextEncryptor derive(String salt) {
        return DataKeys.current(encryptionKey, salt);
    }
}
