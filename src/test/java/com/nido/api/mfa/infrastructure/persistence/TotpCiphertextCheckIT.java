package com.nido.api.mfa.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.Encryptors;
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
        withSecrets(() -> assertThatThrownBy(check::verify).hasMessageContaining("does not decrypt the two-factor secrets"),
            Encryptors.delux(otherKey, FIRST.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"),
            Encryptors.delux(otherKey, SECOND.toString().replace("-", "")).encrypt("JBSWY3DPEHPK3PXP"));
    }
}
