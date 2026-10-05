package com.nido.api.instance.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.instance.InstanceSettingsTestSupport;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class SettingsStoreAdapterIT {

    @Autowired SettingsStoreAdapter store;
    @Autowired JdbcClient jdbc;
    @Autowired TransactionTemplate transactions;
    @Autowired EncryptionKey encryptionKey;

    @AfterEach
    void clean() {
        InstanceSettingsTestSupport.clear(store);
    }

    @Test
    void the_salt_separates_this_key_from_every_other() {
        assertThat(new String(Hex.decode(SettingsStoreAdapter.SALT), StandardCharsets.UTF_8)).isEqualTo("nido-instance-settings");
    }

    @Test
    void what_is_saved_reads_back_and_a_cleared_setting_is_gone() {
        UUID by = UUID.randomUUID();
        store.save(Map.of(SettingKey.MAIL_HOST, Optional.of("smtp.example.com"), SettingKey.SWAGGER, Optional.of("true")), by, Instant.now());
        store.save(Map.of(SettingKey.SWAGGER, Optional.empty()), by, Instant.now());

        assertThat(store.load()).containsExactly(Map.entry(SettingKey.MAIL_HOST, "smtp.example.com"));
        assertThat(jdbc.sql("SELECT updated_by FROM instance_settings WHERE key = 'mail.host'").query(UUID.class).single()).isEqualTo(by);
    }

    @Test
    void the_smtp_password_is_stored_encrypted() {
        store.save(Map.of(SettingKey.MAIL_PASSWORD, Optional.of("s3cret")), null, Instant.now());

        assertThat(jdbc.sql("SELECT value FROM instance_settings WHERE key = 'mail.password'").query(String.class).single())
            .isNotEqualTo("s3cret");
        assertThat(store.load()).containsEntry(SettingKey.MAIL_PASSWORD, "s3cret");
    }

    @Test
    void a_change_is_read_once_committed_and_never_if_rolled_back() {
        store.load();

        transactions.executeWithoutResult(status ->
            store.save(Map.of(SettingKey.MAIL_HOST, Optional.of("committed")), null, Instant.now()));
        assertThat(store.load()).containsEntry(SettingKey.MAIL_HOST, "committed");

        transactions.executeWithoutResult(status -> {
            store.save(Map.of(SettingKey.MAIL_HOST, Optional.of("rolled-back")), null, Instant.now());
            status.setRollbackOnly();
        });
        assertThat(store.load()).containsEntry(SettingKey.MAIL_HOST, "committed");
    }

    @Test
    void a_read_that_raced_a_change_does_not_keep_the_old_values() {
        store.save(Map.of(SettingKey.MAIL_HOST, Optional.of("before")), null, Instant.now());
        AtomicBoolean raced = new AtomicBoolean();
        SettingsStoreAdapter racing = new SettingsStoreAdapter(jdbc, encryptionKey) {
            @Override
            Map<SettingKey, String> read() {
                Map<SettingKey, String> seen = super.read();
                if (raced.compareAndSet(false, true)) {
                    // Another request saves and commits between this one's query and its caching.
                    transactions.executeWithoutResult(status ->
                        save(Map.of(SettingKey.MAIL_HOST, Optional.of("after")), null, Instant.now()));
                }
                return seen;
            }
        };

        assertThat(racing.load()).containsEntry(SettingKey.MAIL_HOST, "before");
        assertThat(racing.load()).containsEntry(SettingKey.MAIL_HOST, "after");
    }

    @Test
    void a_row_from_a_later_version_is_ignored() {
        jdbc.sql("INSERT INTO instance_settings (key, value, updated_at) VALUES ('future.setting', 'x', now())").update();
        try {
            InstanceSettingsTestSupport.clear(store);
            assertThat(store.load()).isEmpty();
        } finally {
            jdbc.sql("DELETE FROM instance_settings WHERE key = 'future.setting'").update();
        }
    }
}
