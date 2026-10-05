package com.nido.api.mail.infrastructure.config;

import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class MailSettingsTest {

    private static MailSettingsInput input(String from, String appUrl) {
        return new MailSettingsInput("smtp.example.com", 587, "starttls", "user", "s3cret", from, appUrl);
    }

    @Test
    void a_complete_configuration_is_kept_with_its_sender_parsed() {
        MailSettings settings = MailSettings.check(input("Nido <nido@example.com>", "https://nido.example.com/")).settings().orElseThrow();

        assertThat(settings.host()).isEqualTo("smtp.example.com");
        assertThat(settings.from().getAddress()).isEqualTo("nido@example.com");
        assertThat(settings.from().getPersonal()).isEqualTo("Nido");
        assertThat(settings.appUrl()).isEqualTo(URI.create("https://nido.example.com"));
        assertThat(settings.security()).isEqualTo(MailSecurity.STARTTLS);
    }

    @Test
    void every_problem_is_named_by_its_field() {
        MailSettings.Check check = MailSettings.check(new MailSettingsInput(" ", null, null, null, null, null, null));

        assertThat(check.settings()).isEmpty();
        assertThat(check.problems()).containsExactlyInAnyOrder(
            new MailSettingsProblem(MailSettingsProblem.Field.HOST, MailSettingsProblem.REQUIRED),
            new MailSettingsProblem(MailSettingsProblem.Field.FROM, MailSettingsProblem.REQUIRED),
            new MailSettingsProblem(MailSettingsProblem.Field.APP_URL, MailSettingsProblem.REQUIRED));
    }

    @Test
    void a_sender_that_is_not_an_address_is_refused() {
        assertThat(MailSettings.check(input("not an address", "https://nido.example.com")).problems())
            .containsExactly(new MailSettingsProblem(MailSettingsProblem.Field.FROM, MailSettingsProblem.INVALID_ADDRESS));
    }

    @Test
    void the_app_url_is_absolute_and_plain_http_is_fine_for_any_host() {
        assertThat(MailSettings.check(input("nido@example.com", "nido.example.com")).problems())
            .containsExactly(new MailSettingsProblem(MailSettingsProblem.Field.APP_URL, MailSettingsProblem.INVALID_URL));
        assertThat(MailSettings.check(input("nido@example.com", "http://192.168.1.10:8080")).settings()).isPresent();
    }

    @Test
    void port_and_security_take_their_defaults_and_are_bounded() {
        MailSettings defaults = MailSettings.check(new MailSettingsInput("smtp.example.com", null, null, null, null,
            "nido@example.com", "https://nido.example.com")).settings().orElseThrow();
        assertThat(defaults.port()).isEqualTo(587);
        assertThat(defaults.security()).isEqualTo(MailSecurity.STARTTLS);

        assertThat(MailSettings.check(new MailSettingsInput("smtp.example.com", 70_000, "ssl", null, null,
            "nido@example.com", "https://nido.example.com")).problems()).containsExactlyInAnyOrder(
            new MailSettingsProblem(MailSettingsProblem.Field.PORT, MailSettingsProblem.OUT_OF_RANGE),
            new MailSettingsProblem(MailSettingsProblem.Field.SECURITY, MailSettingsProblem.UNKNOWN_SECURITY));
    }

    @Test
    void username_and_password_go_together() {
        assertThat(MailSettings.check(new MailSettingsInput("smtp.example.com", 587, "starttls", "user", " ",
            "nido@example.com", "https://nido.example.com")).problems())
            .containsExactly(new MailSettingsProblem(MailSettingsProblem.Field.PASSWORD, MailSettingsProblem.CREDENTIALS_GO_TOGETHER));
        assertThat(MailSettings.check(new MailSettingsInput("smtp.example.com", 587, "starttls", null, "s3cret",
            "nido@example.com", "https://nido.example.com")).problems())
            .containsExactly(new MailSettingsProblem(MailSettingsProblem.Field.USERNAME, MailSettingsProblem.CREDENTIALS_GO_TOGETHER));
    }

    @Test
    void the_password_never_shows() {
        assertThat(MailSettings.check(input("nido@example.com", "https://nido.example.com")).settings().orElseThrow().toString())
            .doesNotContain("s3cret");
        assertThat(input("nido@example.com", "https://nido.example.com").toString()).doesNotContain("s3cret");
    }
}
