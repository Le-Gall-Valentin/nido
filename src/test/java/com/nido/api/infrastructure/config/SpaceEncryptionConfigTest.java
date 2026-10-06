package com.nido.api.infrastructure.config;

import com.nido.api.shared.security.EncryptionKey;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceEncryptionConfigTest {

    private static final EncryptionKey KEY = new EncryptionKey("test-encryption-secret-32chars!!");

    // A 32-hex-char stand-in salt, deterministic per space id — the real use case reads the space's
    // stored random salt; this double only needs distinct, valid hex per id.
    private static String saltFor(UUID spaceId) {
        String hex = spaceId.toString().replace("-", "");
        return (hex + hex).substring(0, 32);
    }

    private static SpaceEncryptorFactory factory() {
        GetSpaceEncryptionSaltUseCase salts = SpaceEncryptionConfigTest::saltFor;
        return new SpaceEncryptionConfig().spaceEncryptorFactory(new SpaceKeyCache(KEY), salts);
    }

    @Test
    void what_finance_and_the_calendar_encrypted_before_still_decrypts() {
        UUID spaceId = UUID.randomUUID();
        // Exactly what FinanceEncryptionConfig and CalendarEncryptionConfig derived up to 0.13.0.
        String before = Encryptors.delux(KEY.value(), saltFor(spaceId)).encrypt("Loyer octobre");

        assertThat(factory().forSpace(spaceId).decrypt(before)).isEqualTo("Loyer octobre");
    }

    @Test
    void the_same_space_gets_the_cached_encryptor() {
        SpaceEncryptorFactory factory = factory();
        UUID spaceId = UUID.randomUUID();

        assertThat(factory.forSpace(spaceId)).isSameAs(factory.forSpace(spaceId));
    }

    @Test
    void two_spaces_get_two_encryptors() {
        SpaceEncryptorFactory factory = factory();

        TextEncryptor first = factory.forSpace(UUID.randomUUID());
        TextEncryptor second = factory.forSpace(UUID.randomUUID());

        assertThat(first).isNotSameAs(second);
    }

    @Test
    void the_same_text_encrypts_differently_for_two_spaces() {
        SpaceEncryptorFactory factory = factory();

        assertThat(factory.forSpace(UUID.randomUUID()).encrypt("Loyer"))
            .isNotEqualTo(factory.forSpace(UUID.randomUUID()).encrypt("Loyer"));
    }

    @Test
    void a_value_encrypted_for_a_space_decrypts_back() {
        SpaceEncryptorFactory factory = factory();
        UUID spaceId = UUID.randomUUID();

        String encrypted = factory.forSpace(spaceId).encrypt("Courses Carrefour 45.30€");

        assertThat(factory.forSpace(spaceId).decrypt(encrypted)).isEqualTo("Courses Carrefour 45.30€");
    }
}
