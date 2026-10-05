package com.nido.api.instance.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsResolutionTest {

    @Test
    void the_environment_wins_then_the_database_then_the_default() {
        EffectiveSettings settings = SettingsResolution.resolve(
            Map.of(SettingKey.ACCESS_TOKEN_MINUTES, "20"),
            Map.of(SettingKey.ACCESS_TOKEN_MINUTES, "30", SettingKey.REFRESH_TOKEN_DAYS, "7")).settings();

        assertThat(settings.get(SettingKey.ACCESS_TOKEN_MINUTES)).isEqualTo(
            new EffectiveSetting(SettingKey.ACCESS_TOKEN_MINUTES, "20", SettingSource.ENVIRONMENT));
        assertThat(settings.get(SettingKey.REFRESH_TOKEN_DAYS)).isEqualTo(
            new EffectiveSetting(SettingKey.REFRESH_TOKEN_DAYS, "7", SettingSource.DATABASE));
        assertThat(settings.get(SettingKey.SWAGGER)).isEqualTo(
            new EffectiveSetting(SettingKey.SWAGGER, "false", SettingSource.DEFAULT));
        assertThat(settings.integer(SettingKey.ACCESS_TOKEN_MINUTES)).isEqualTo(20);
        assertThat(settings.flag(SettingKey.SWAGGER)).isFalse();
    }

    @Test
    void one_mail_variable_puts_the_whole_mail_group_in_the_hands_of_the_environment() {
        EffectiveSettings settings = SettingsResolution.resolve(
            Map.of(SettingKey.MAIL_HOST, "smtp.example.com"),
            Map.of(SettingKey.MAIL_PASSWORD, "stored-password", SettingKey.MAIL_PORT, "2525")).settings();

        for (SettingKey key : SettingGroup.MAIL.keys()) {
            assertThat(settings.locked(key)).as(key.code()).isTrue();
        }
        assertThat(settings.text(SettingKey.MAIL_PASSWORD)).isEmpty();
        assertThat(settings.integer(SettingKey.MAIL_PORT)).isEqualTo(587);
        assertThat(settings.locked(SettingKey.PUBLIC_URL)).isFalse();
    }

    @Test
    void a_stored_value_that_no_longer_holds_is_ignored_and_reported() {
        SettingsResolution.Resolved resolved = SettingsResolution.resolve(Map.of(), Map.of(SettingKey.REFRESH_TOKEN_DAYS, "9999"));

        assertThat(resolved.settings().get(SettingKey.REFRESH_TOKEN_DAYS).source()).isEqualTo(SettingSource.DEFAULT);
        assertThat(resolved.ignored()).containsExactly(SettingKey.REFRESH_TOKEN_DAYS);
    }

    @Test
    void a_secret_never_shows_in_a_log_line() {
        EffectiveSettings settings = SettingsResolution.resolve(Map.of(), Map.of(SettingKey.MAIL_PASSWORD, "s3cret")).settings();

        assertThat(settings.get(SettingKey.MAIL_PASSWORD).toString()).doesNotContain("s3cret");
        assertThat(MailDraft.of(settings).toString()).doesNotContain("s3cret");
    }
}
