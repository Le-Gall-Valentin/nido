package com.nido.api.authentication.infrastructure.config;

import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingsResolution;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PublicUrlAdapterTest {

    private final Map<SettingKey, String> stored = new EnumMap<>(SettingKey.class);
    private final GetEffectiveSettingsQuery settings = () -> SettingsResolution.resolve(Map.of(), stored).settings();

    @Test
    void the_public_address_is_the_setting_when_there_is_one_and_follows_a_change_at_once() {
        PublicUrlAdapter adapter = new PublicUrlAdapter(settings);

        assertThat(adapter.publicUrl()).isEmpty();
        stored.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        assertThat(adapter.publicUrl()).contains("https://nido.example.com");
    }
}
