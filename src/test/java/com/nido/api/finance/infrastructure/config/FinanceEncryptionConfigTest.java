package com.nido.api.finance.infrastructure.config;

import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceEncryptionConfigTest {

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

    // A 32-hex-char stand-in salt, deterministic per space id — a real GetSpaceEncryptionSaltUseCase
    // reads the space's stored random salt (Task 3); this test double only needs distinct, valid hex per id.
    private static String saltFor(UUID spaceId) {
        String hex = spaceId.toString().replace("-", "");
        return (hex + hex).substring(0, 32);
    }

    @Test
    void forSpace_sameSpaceId_returnsCachedEncryptorInstance() {
        GetSpaceEncryptionSaltUseCase saltUseCase = FinanceEncryptionConfigTest::saltFor;
        FinanceEncryptorFactory factory = new FinanceEncryptionConfig().financeEncryptorFactory(propertiesWith(SECRET_32), saltUseCase);
        UUID spaceId = UUID.randomUUID();

        TextEncryptor first = factory.forSpace(spaceId);
        TextEncryptor second = factory.forSpace(spaceId);

        assertThat(first).isSameAs(second);
    }

    @Test
    void forSpace_differentSpaceIds_returnDifferentEncryptors() {
        GetSpaceEncryptionSaltUseCase saltUseCase = FinanceEncryptionConfigTest::saltFor;
        FinanceEncryptorFactory factory = new FinanceEncryptionConfig().financeEncryptorFactory(propertiesWith(SECRET_32), saltUseCase);

        TextEncryptor forSpace1 = factory.forSpace(UUID.randomUUID());
        TextEncryptor forSpace2 = factory.forSpace(UUID.randomUUID());

        assertThat(forSpace1).isNotSameAs(forSpace2);
    }

    @Test
    void encrypting_the_same_plaintext_for_two_different_spaces_produces_different_ciphertext() {
        GetSpaceEncryptionSaltUseCase saltUseCase = FinanceEncryptionConfigTest::saltFor;
        FinanceEncryptorFactory factory = new FinanceEncryptionConfig().financeEncryptorFactory(propertiesWith(SECRET_32), saltUseCase);

        String encryptedForSpace1 = factory.forSpace(UUID.randomUUID()).encrypt("Loyer");
        String encryptedForSpace2 = factory.forSpace(UUID.randomUUID()).encrypt("Loyer");

        assertThat(encryptedForSpace1).isNotEqualTo(encryptedForSpace2);
    }

    @Test
    void a_value_encrypted_for_a_space_decrypts_back_to_the_original_plaintext() {
        GetSpaceEncryptionSaltUseCase saltUseCase = FinanceEncryptionConfigTest::saltFor;
        FinanceEncryptorFactory factory = new FinanceEncryptionConfig().financeEncryptorFactory(propertiesWith(SECRET_32), saltUseCase);
        UUID spaceId = UUID.randomUUID();

        String encrypted = factory.forSpace(spaceId).encrypt("Courses Carrefour 45.30€");

        assertThat(factory.forSpace(spaceId).decrypt(encrypted)).isEqualTo("Courses Carrefour 45.30€");
    }
}
