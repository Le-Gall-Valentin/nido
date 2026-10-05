package com.nido.api.authentication.infrastructure.config;

import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingsResolution;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionSettingsAdapterTest {

    private final Map<SettingKey, String> stored = new EnumMap<>(SettingKey.class);
    private final GetEffectiveSettingsQuery settings = () -> SettingsResolution.resolve(Map.of(), stored).settings();

    private SessionSettingsAdapter adapter(Boolean secure) {
        NidoProperties properties = new NidoProperties(null, null, new NidoProperties.CookieProperties(secure), null, null, null, null);
        return new SessionSettingsAdapter(settings, properties);
    }

    @Test
    void durations_take_their_defaults_and_follow_a_change_at_once() {
        SessionSettingsAdapter adapter = adapter(null);
        assertThat(adapter.accessTokenMinutes()).isEqualTo(15);
        assertThat(adapter.refreshTokenExpiryDays()).isEqualTo(30);

        stored.put(SettingKey.ACCESS_TOKEN_MINUTES, "60");
        stored.put(SettingKey.REFRESH_TOKEN_DAYS, "7");

        assertThat(adapter.accessTokenMinutes()).isEqualTo(60);
        assertThat(adapter.refreshTokenExpiryDays()).isEqualTo(7);
    }

    @Test
    void cookies_follow_the_public_address() {
        assertThat(adapter(null).secureCookies()).as("no address known: as before this version").isTrue();
        stored.put(SettingKey.PUBLIC_URL, "http://192.168.1.10:8080");
        assertThat(adapter(null).secureCookies()).isFalse();
        stored.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        assertThat(adapter(null).secureCookies()).isTrue();
    }

    @Test
    void nido_cookie_secure_has_the_last_word_whatever_the_address() {
        stored.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        assertThat(adapter(false).secureCookies()).as("the way back in after an https address saved too early").isFalse();
        stored.put(SettingKey.PUBLIC_URL, "http://192.168.1.10:8080");
        assertThat(adapter(true).secureCookies()).isTrue();
    }
}
