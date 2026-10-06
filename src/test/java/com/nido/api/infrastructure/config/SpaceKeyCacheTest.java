package com.nido.api.infrastructure.config;

import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceKeyCacheTest {

    private static final String SALT = "00112233445566778899aabbccddeeff";

    private final SpaceKeyCache keys = new SpaceKeyCache(new EncryptionKey("test-encryption-secret-32chars!!"));

    @Test
    void what_is_encrypted_for_a_space_not_yet_inserted_reads_back_once_it_has_an_id() {
        String encrypted = keys.forNewSpace(SALT).encrypt("Famille Le Gall");

        assertThat(keys.forSpace(UUID.randomUUID(), () -> SALT).decrypt(encrypted)).isEqualTo("Famille Le Gall");
    }

    @Test
    void the_salt_is_not_read_again_while_the_key_is_cached() {
        AtomicInteger reads = new AtomicInteger();
        UUID spaceId = UUID.randomUUID();

        keys.forSpace(spaceId, () -> { reads.incrementAndGet(); return SALT; });
        keys.forSpace(spaceId, () -> { reads.incrementAndGet(); return SALT; });

        assertThat(reads).hasValue(1);
    }
}
