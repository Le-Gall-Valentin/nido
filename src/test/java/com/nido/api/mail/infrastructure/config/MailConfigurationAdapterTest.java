package com.nido.api.mail.infrastructure.config;

import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingsResolution;
import com.nido.api.mail.domain.model.ActiveMail;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MailConfigurationAdapterTest {

    private final Map<SettingKey, String> stored = new EnumMap<>(SettingKey.class);
    private final GetEffectiveSettingsQuery settings = () -> SettingsResolution.resolve(Map.of(), stored).settings();
    private final MailConfigurationAdapter adapter = new MailConfigurationAdapter(settings);

    private void configure() {
        stored.put(SettingKey.MAIL_HOST, "smtp.example.com");
        stored.put(SettingKey.MAIL_FROM, "Nido <nido@example.com>");
        stored.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
    }

    @Test
    void no_host_means_mail_is_off() {
        assertThat(adapter.active()).isEmpty();
        assertThat(adapter.settings()).isEmpty();
    }

    @Test
    void a_configuration_that_holds_turns_mail_on_with_links_to_the_public_address() {
        configure();

        assertThat(adapter.active()).contains(new ActiveMail("https://nido.example.com"));
    }

    @Test
    void a_configuration_that_does_not_hold_keeps_mail_off() {
        configure();
        stored.put(SettingKey.MAIL_FROM, "not an address");

        assertThat(adapter.active()).isEmpty();
    }

    @Test
    void the_same_settings_are_checked_once() {
        configure();

        assertThat(adapter.settings().orElseThrow()).isSameAs(adapter.settings().orElseThrow());
    }

    @Test
    void a_change_is_seen_at_once() {
        configure();
        adapter.settings();
        stored.put(SettingKey.MAIL_HOST, "smtp.other.com");

        assertThat(adapter.settings().orElseThrow().host()).isEqualTo("smtp.other.com");
    }
}
