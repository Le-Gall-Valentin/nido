package com.nido.api.mfa.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.infrastructure.sealing.FixedLegacyFormats;
import com.nido.api.infrastructure.sealing.OwnKeyEncryptors;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Each test works on two_factor_methods inside a transaction it rolls back: the other tests never see its rows. */
@IntegrationTestConfig
class TotpCiphertextCheckIT {

    private static final UUID FIRST = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-4000-8000-000000000002");

    @Autowired TotpCiphertextCheck check;
    @Autowired TotpEncryptorFactory encryptors;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    /**
     * The check as the first start of 0.16.0 runs it, earlier formats still taken. The context's own has them closed: it
     * would refuse a secret of an earlier format whatever the key, and prove nothing about the key.
     */
    private TotpCiphertextCheck beforeEveryValueIsCurrent() {
        OwnKeyEncryptors open = new OwnKeyEncryptors(new EncryptionKey(TestSpaces.ENCRYPTION_KEY), FixedLegacyFormats.open());
        return new TotpCiphertextCheck(jdbc, user -> open.forSalt(user.toString().replace("-", "")), FixedLegacyFormats.open());
    }

    private void withSecrets(Runnable test, String firstSecret, String secondSecret) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            jdbc.sql("DELETE FROM two_factor_methods").update();
            secret(FIRST, "totp-check-first", firstSecret);
            secret(SECOND, "totp-check-second", secondSecret);
            test.run();
        });
    }

    private void secret(UUID user, String username, String secret) {
        jdbc.sql("INSERT INTO users (id, username, email, role) VALUES (:id, :username, :email, 'USER')")
            .param("id", user).param("username", username).param("email", username + "@example.fr").update();
        jdbc.sql("INSERT INTO two_factor_methods (user_id, method, secret) VALUES (:id, 'APP', :secret)")
            .param("id", user).param("secret", secret).update();
    }

    @Test
    void one_damaged_secret_among_good_ones_is_not_taken_for_a_wrong_key() {
        withSecrets(() -> assertThatCode(check::verify).doesNotThrowAnyException(),
            "not-a-ciphertext", encryptors.forUser(SECOND).encrypt("JBSWY3DPEHPK3PXP"));
    }

    @Test
    void secrets_none_of_which_opens_refuse_the_key() {
        String otherKey = "a-key-someone-typed-by-mistake-32-chars+";
        withSecrets(() -> assertThatThrownBy(beforeEveryValueIsCurrent()::verify).hasMessageContaining("does not decrypt the two-factor secrets"),
            LegacyKeys.writer(otherKey, FIRST.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"),
            LegacyKeys.writer(otherKey, SECOND.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"));
    }

    @Test
    void while_earlier_formats_are_taken_secrets_of_the_legacy_key_prove_it() {
        withSecrets(() -> assertThatCode(beforeEveryValueIsCurrent()::verify).doesNotThrowAnyException(),
            LegacyKeys.writer(TestSpaces.ENCRYPTION_KEY, FIRST.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"),
            LegacyKeys.writer(TestSpaces.ENCRYPTION_KEY, SECOND.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"));
    }

    @Test
    void once_earlier_formats_are_closed_secrets_of_one_written_since_are_not_taken_for_a_wrong_key() {
        // Written by someone with access to the database, ahead of the genuine secrets in the order samples are taken.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            jdbc.sql("DELETE FROM two_factor_methods").update();
            for (int i = 1; i <= 5; i++) {
                UUID planted = UUID.fromString("00000000-0000-4000-8000-00000000000" + i);
                secret(planted, "totp-check-planted-" + i,
                    LegacyKeys.writer(TestSpaces.ENCRYPTION_KEY, planted.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"));
            }
            UUID genuine = UUID.fromString("00000000-0000-4000-8000-000000000006");
            secret(genuine, "totp-check-genuine", encryptors.forUser(genuine).encrypt("JBSWY3DPEHPK3PXP"));

            assertThatCode(check::verify).doesNotThrowAnyException();
        });
    }

    @Test
    void current_secrets_of_another_key_refuse_it() {
        EncryptionKey other = new EncryptionKey("a-key-someone-typed-by-mistake-32-chars+");
        withSecrets(() -> assertThatThrownBy(check::verify).hasMessageContaining("does not decrypt the two-factor secrets"),
            new OwnKeyEncryptors(other, FixedLegacyFormats.closed()).forSalt(FIRST.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"),
            new OwnKeyEncryptors(other, FixedLegacyFormats.closed()).forSalt(SECOND.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"));
    }
}
