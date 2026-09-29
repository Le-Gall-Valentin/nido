package com.nido.api.mail.infrastructure.config;

import com.nido.api.infrastructure.config.MailProperties;
import com.nido.api.infrastructure.config.MailProperties.Security;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailSettingsTest {

    private static MailProperties properties(String from, String appUrl) {
        return new MailProperties("smtp.example.com", 587, Security.STARTTLS, "user", "s3cret", from, appUrl);
    }

    @Test
    void a_complete_configuration_is_parsed() {
        MailSettings settings = MailSettings.from(properties("Nido <nido@example.com>", "https://nido.example.com/"));

        assertThat(settings.host()).isEqualTo("smtp.example.com");
        assertThat(settings.from().getAddress()).isEqualTo("nido@example.com");
        assertThat(settings.from().getPersonal()).isEqualTo("Nido");
        assertThat(settings.appUrl()).isEqualTo(URI.create("https://nido.example.com"));
    }

    @Test
    void every_missing_value_is_named_at_once() {
        assertThatThrownBy(() -> MailSettings.from(properties(" ", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NIDO_SMTP_HOST is set")
            .hasMessageContaining("NIDO_MAIL_FROM is required")
            .hasMessageContaining("NIDO_APP_URL is required");
    }

    @Test
    void a_sender_that_is_not_an_address_is_refused() {
        assertThatThrownBy(() -> MailSettings.from(properties("not an address", "https://nido.example.com")))
            .hasMessageContaining("NIDO_MAIL_FROM is not a valid address");
    }

    @Test
    void the_app_url_must_be_absolute_and_https_outside_localhost() {
        assertThatThrownBy(() -> MailSettings.from(properties("nido@example.com", "nido.example.com")))
            .hasMessageContaining("NIDO_APP_URL must be an absolute http(s) URL");
        assertThatThrownBy(() -> MailSettings.from(properties("nido@example.com", "http://nido.example.com")))
            .hasMessageContaining("NIDO_APP_URL must use https");
        assertThat(MailSettings.from(properties("nido@example.com", "http://localhost:5173")).appUrl())
            .isEqualTo(URI.create("http://localhost:5173"));
        assertThat(MailSettings.from(properties("nido@example.com", "http://127.0.0.1:8080")).appUrl())
            .isEqualTo(URI.create("http://127.0.0.1:8080"));
    }

    @Test
    void no_security_given_means_starttls_and_blank_credentials_mean_none() {
        MailSettings settings = MailSettings.from(new MailProperties("smtp.example.com", 587, null, " ", "",
            "nido@example.com", "https://nido.example.com"));

        assertThat(settings.security()).isEqualTo(Security.STARTTLS);
        assertThat(settings.username()).isNull();
        assertThat(settings.password()).isNull();
    }

    @Test
    void the_password_never_appears_in_the_settings_string() {
        assertThat(MailSettings.from(properties("nido@example.com", "https://nido.example.com")).toString())
            .doesNotContain("s3cret");
    }
}
