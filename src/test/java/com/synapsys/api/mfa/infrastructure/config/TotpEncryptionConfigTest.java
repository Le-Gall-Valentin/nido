package com.synapsys.api.mfa.infrastructure.config;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TotpEncryptionConfigTest {

    private static final String SECRET_32 = "test-encryption-secret-32chars!!";

    private SynapsysProperties propertiesWith(String encSecret) {
        return new SynapsysProperties(
            new SynapsysProperties.JwtProperties("jwt-secret-at-least-32-chars-long!", 15, "test", "test"),
            new SynapsysProperties.RefreshTokenProperties(30, "0 0 3 * * *"),
            new SynapsysProperties.CookieProperties(false),
            null,
            new SynapsysProperties.CorsProperties(List.of()),
            new SynapsysProperties.RateLimitProperties(List.of()),
            new SynapsysProperties.EncryptionProperties(encSecret),
            null
        );
    }

    @Test
    void forUser_sameUserId_returnsCachedEncryptorInstance() {
        TotpEncryptorFactory factory = new TotpEncryptionConfig().totpEncryptorFactory(propertiesWith(SECRET_32));
        UUID userId = UUID.randomUUID();

        TextEncryptor first = factory.forUser(userId);
        TextEncryptor second = factory.forUser(userId);

        assertThat(first).isSameAs(second);
    }

    @Test
    void forUser_differentUserIds_returnDifferentEncryptors() {
        TotpEncryptorFactory factory = new TotpEncryptionConfig().totpEncryptorFactory(propertiesWith(SECRET_32));

        TextEncryptor forUser1 = factory.forUser(UUID.randomUUID());
        TextEncryptor forUser2 = factory.forUser(UUID.randomUUID());

        assertThat(forUser1).isNotSameAs(forUser2);
    }
}