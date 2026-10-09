package com.nido.api.mfa.infrastructure.config;

import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.infrastructure.sealing.RekeyedColumn;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpEncryptionConfigTest {

    private static final String SECRET_32 = "test-encryption-secret-32chars!!";

    private static EncryptionKey keyOf(String secret) {
        return new EncryptionKey(secret);
    }

    @Test
    void forUser_sameUserId_returnsCachedEncryptorInstance() {
        TotpEncryptorFactory factory = new TotpEncryptionConfig().totpEncryptorFactory(keyOf(SECRET_32));
        UUID userId = UUID.randomUUID();

        TextEncryptor first = factory.forUser(userId);
        TextEncryptor second = factory.forUser(userId);

        assertThat(first).isSameAs(second);
    }

    @Test
    void forUser_differentUserIds_returnDifferentEncryptors() {
        TotpEncryptorFactory factory = new TotpEncryptionConfig().totpEncryptorFactory(keyOf(SECRET_32));

        TextEncryptor forUser1 = factory.forUser(UUID.randomUUID());
        TextEncryptor forUser2 = factory.forUser(UUID.randomUUID());

        assertThat(forUser1).isNotSameAs(forUser2);
    }

    @Test
    void a_secret_is_written_with_the_current_key_of_its_user() {
        TotpEncryptorFactory factory = new TotpEncryptionConfig().totpEncryptorFactory(keyOf(TestSpaces.ENCRYPTION_KEY));
        UUID jane = UUID.fromString("00000000-0000-4000-8000-000000000001");

        String stored = factory.forUser(jane).encrypt("JBSWY3DPEHPK3PXP");

        assertThat(stored).startsWith("k2:");
        assertThat(factory.forUser(jane).decrypt(stored)).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThatThrownBy(() -> factory.forUser(UUID.randomUUID()).decrypt(stored)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void a_secret_written_before_0_16_still_reads_an_enrolment_waiting_in_redis_included() {
        TotpEncryptorFactory factory = new TotpEncryptionConfig().totpEncryptorFactory(keyOf(TestSpaces.ENCRYPTION_KEY));
        String legacy = LegacyKeys.writer(TestSpaces.ENCRYPTION_KEY, "00000000000040008000000000000001").encrypt("JBSWY3DPEHPK3PXP");

        assertThat(factory.forUser(UUID.fromString("00000000-0000-4000-8000-000000000001")).decrypt(legacy))
            .isEqualTo("JBSWY3DPEHPK3PXP");
    }

    @Test
    void the_app_secrets_are_declared_for_the_migration() {
        TotpEncryptionConfig config = new TotpEncryptionConfig();
        RekeyedColumn column = config.totpRekeyedColumns(config.totpEncryptorFactory(keyOf(SECRET_32))).columns().getFirst();

        assertThat(column).hasToString("two_factor_methods.secret");
    }
}
