package com.nido.api.instance.infrastructure.persistence;

import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.shared.security.EncryptionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The saved settings, read from Postgres once and kept in memory until a change commits. Settings are
 * asked for on every token issued and every mail written, so a query each time would be a query per
 * request for ten rows that change a few times a year. One instance of Nido runs at a time — the
 * derived keys and the setup code are in memory already — so forgetting the cache locally is enough.
 */
@Component
public class SettingsStoreAdapter implements SettingsStorePort {

    private static final Logger log = LoggerFactory.getLogger(SettingsStoreAdapter.class);

    /** "nido-instance-settings", hex-encoded as Encryptors expects. Not a secret: it separates keys. */
    static final String SALT = "6e69646f2d696e7374616e63652d73657474696e6773";

    private final JdbcClient jdbc;
    private final TextEncryptor encryptor;
    /**
     * The values, or none to read again. A fresh holder at each change: a read kept only if the holder
     * it started from is still in place, so a query that ran before a change committed never caches.
     */
    private record Cached(Map<SettingKey, String> values) {}

    private final AtomicReference<Cached> cache = new AtomicReference<>(new Cached(null));

    @Autowired
    public SettingsStoreAdapter(JdbcClient jdbc, EncryptionKey encryptionKey) {
        this(jdbc, Encryptors.delux(encryptionKey.value(), SALT));
    }

    SettingsStoreAdapter(JdbcClient jdbc, TextEncryptor encryptor) {
        this.jdbc = jdbc;
        this.encryptor = encryptor;
    }

    @Override
    public Map<SettingKey, String> load() {
        Cached seen = cache.get();
        if (seen.values() != null) {
            return seen.values();
        }
        Map<SettingKey, String> values = read();
        cache.compareAndSet(seen, new Cached(values));
        return values;
    }

    /** The rows as they are now. */
    Map<SettingKey, String> read() {
        EnumMap<SettingKey, String> loaded = new EnumMap<>(SettingKey.class);
        jdbc.sql("SELECT key, value FROM instance_settings")
            .query((rs, rowNum) -> Map.entry(rs.getString("key"), rs.getString("value")))
            .list()
            .forEach(row -> SettingKey.fromCode(row.getKey()).ifPresent(key -> decode(key, row.getValue())
                .ifPresent(value -> loaded.put(key, value))));
        return Collections.unmodifiableMap(loaded);
    }

    private Optional<String> decode(SettingKey key, String stored) {
        if (!key.secret()) {
            return Optional.of(stored);
        }
        try {
            return Optional.of(encryptor.decrypt(stored));
        } catch (RuntimeException e) {
            log.error("The saved {} cannot be read with the current key and is ignored: {}", key.code(), e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void save(Map<SettingKey, Optional<String>> changes, UUID by, Instant at) {
        changes.forEach((key, value) -> value.ifPresentOrElse(
            v -> jdbc.sql("""
                    INSERT INTO instance_settings (key, value, updated_at, updated_by)
                    VALUES (:key, :value, :at, :by)
                    ON CONFLICT (key) DO UPDATE
                    SET value = EXCLUDED.value, updated_at = EXCLUDED.updated_at, updated_by = EXCLUDED.updated_by
                    """)
                .param("key", key.code())
                .param("value", key.secret() ? encryptor.encrypt(v) : v)
                .param("at", OffsetDateTime.ofInstant(at, ZoneOffset.UTC))
                .param("by", by, Types.OTHER)
                .update(),
            () -> jdbc.sql("DELETE FROM instance_settings WHERE key = :key").param("key", key.code()).update()));
        forgetWhenDone();
    }

    private void forgetWhenDone() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cache.set(new Cached(null));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                cache.set(new Cached(null));
            }
        });
    }
}
