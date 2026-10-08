package com.nido.api.mfa.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class TwoFactorMethodRepositoryAdapterIT {

    @Autowired TwoFactorMethodStorePort store;
    @Autowired JdbcClient jdbc;

    private UUID jane;
    private UUID john;

    @BeforeEach
    void accounts() {
        jdbc.sql("DELETE FROM two_factor_methods").update();
        jdbc.sql("DELETE FROM users WHERE username IN ('tfm-jane', 'tfm-john')").update();
        jane = account("tfm-jane");
        john = account("tfm-john");
    }

    private UUID account(String username) {
        return jdbc.sql("INSERT INTO users (username, email, role) VALUES (:u, :e, 'USER') RETURNING id")
            .param("u", username).param("e", username + "@example.fr").query(UUID.class).single();
    }

    @Test
    void an_app_secret_is_stored_encrypted_and_read_back_in_clear() {
        store.enable(jane, TwoFactorMethod.APP, "JBSWY3DPEHPK3PXP");

        String stored = jdbc.sql("SELECT secret FROM two_factor_methods WHERE user_id = :id").param("id", jane)
            .query(String.class).single();
        assertThat(stored).isNotEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(store.appSecret(jane)).contains("JBSWY3DPEHPK3PXP");
        assertThat(store.activeMethods(jane)).containsExactly(TwoFactorMethod.APP);
    }

    @Test
    void the_mail_method_has_no_secret() {
        store.enable(jane, TwoFactorMethod.MAIL, null);

        assertThat(store.activeMethods(jane)).containsExactly(TwoFactorMethod.MAIL);
        assertThat(store.appSecret(jane)).isEmpty();
    }

    @Test
    void disabling_says_whether_the_method_was_on() {
        store.enable(jane, TwoFactorMethod.MAIL, null);

        assertThat(store.disable(jane, TwoFactorMethod.MAIL)).isTrue();
        assertThat(store.disable(jane, TwoFactorMethod.MAIL)).isFalse();
        assertThat(store.activeMethods(jane)).isEmpty();
    }

    @Test
    void every_account_asked_for_is_answered_even_without_a_method() {
        store.enable(jane, TwoFactorMethod.MAIL, null);
        store.enable(jane, TwoFactorMethod.APP, "JBSWY3DPEHPK3PXP");

        var methods = store.activeMethodsAmong(List.of(jane, john));

        assertThat(methods.get(jane)).containsExactly(TwoFactorMethod.APP, TwoFactorMethod.MAIL);
        assertThat(methods.get(john)).isEmpty();
        assertThat(store.activeMethodsAmong(List.of())).isEmpty();
    }

    @Test
    void deleting_an_account_data_takes_every_method() {
        store.enable(jane, TwoFactorMethod.MAIL, null);
        store.enable(jane, TwoFactorMethod.APP, "JBSWY3DPEHPK3PXP");

        store.deleteAll(jane);

        assertThat(store.activeMethods(jane)).isEmpty();
    }
}
