package com.nido.api.mfa.infrastructure.config;

import com.nido.api.infrastructure.config.NidoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TotpEncryptionConfigTest {

    private static final String SECRET_32 = "test-encryption-secret-32chars!!";

    private NidoProperties propertiesWith(String encSecret) {
        return new NidoProperties(
            new NidoProperties.JwtProperties("jwt-secret-at-least-32-chars-long!", 15, "test", "test"),
            new NidoProperties.RefreshTokenProperties(30, "0 0 3 * * *"),
            new NidoProperties.CookieProperties(false),
            null,
            new NidoProperties.CorsProperties(List.of()),
            new NidoProperties.RateLimitProperties(List.of()),
            new NidoProperties.EncryptionProperties(encSecret),
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