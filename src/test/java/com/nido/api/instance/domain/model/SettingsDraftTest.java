package com.nido.api.instance.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsDraftTest {

    @Test
    void a_typed_value_is_checked_and_normalized() {
        SettingsDraft draft = new SettingsDraft()
            .set(SettingKey.MAIL_SECURITY, "TLS")
            .set(SettingKey.MAIL_PORT, "abc");

        assertThat(draft.changes()).containsEntry(SettingKey.MAIL_SECURITY, Optional.of("tls"));
        assertThat(draft.problems()).containsExactly(new SettingProblem(SettingKey.MAIL_PORT, SettingProblem.NOT_A_NUMBER));
    }

    @Test
    void a_blank_value_clears_the_setting_but_a_blank_secret_keeps_the_stored_one() {
        SettingsDraft draft = new SettingsDraft()
            .set(SettingKey.MAIL_USERNAME, "")
            .set(SettingKey.MAIL_PASSWORD, "  ");

        assertThat(draft.changes()).containsEntry(SettingKey.MAIL_USERNAME, Optional.empty())
            .doesNotContainKey(SettingKey.MAIL_PASSWORD);
    }

    @Test
    void the_public_address_is_never_emptied_its_cookies_would_turn_secure_and_lock_http_out() {
        SettingsDraft typed = new SettingsDraft().set(SettingKey.PUBLIC_URL, "  ");
        SettingsDraft reset = new SettingsDraft().clear(SettingKey.PUBLIC_URL);

        for (SettingsDraft draft : new SettingsDraft[]{typed, reset}) {
            assertThat(draft.changes()).doesNotContainKey(SettingKey.PUBLIC_URL);
            assertThat(draft.problems()).containsExactly(new SettingProblem(SettingKey.PUBLIC_URL, SettingProblem.REQUIRED));
        }
    }

    @Test
    void the_draft_applies_over_what_is_stored() {
        SettingsDraft draft = new SettingsDraft().set(SettingKey.MAIL_HOST, "smtp.new").clear(SettingKey.MAIL_PORT);

        assertThat(draft.applyTo(Map.of(SettingKey.MAIL_HOST, "smtp.old", SettingKey.MAIL_PORT, "2525", SettingKey.SWAGGER, "true")))
            .containsOnly(Map.entry(SettingKey.MAIL_HOST, "smtp.new"), Map.entry(SettingKey.SWAGGER, "true"));
    }
}
