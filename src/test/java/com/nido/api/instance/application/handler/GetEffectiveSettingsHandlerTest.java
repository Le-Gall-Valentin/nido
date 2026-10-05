package com.nido.api.instance.application.handler;

import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingSource;
import com.nido.api.instance.domain.port.out.EnvironmentSettingsPort;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetEffectiveSettingsHandlerTest {

    @Test
    void it_resolves_the_environment_over_the_store() {
        EnvironmentSettingsPort environment = () -> Map.of(SettingKey.SWAGGER, "true");
        SettingsStorePort store = new SettingsStorePort() {
            @Override public Map<SettingKey, String> load() { return Map.of(SettingKey.SWAGGER, "false", SettingKey.REFRESH_TOKEN_DAYS, "500"); }
            @Override public void save(Map<SettingKey, Optional<String>> changes, UUID by, Instant at) {}
        };

        var settings = new GetEffectiveSettingsHandler(environment, store).current();

        assertThat(settings.flag(SettingKey.SWAGGER)).isTrue();
        assertThat(settings.source(SettingKey.REFRESH_TOKEN_DAYS)).isEqualTo(SettingSource.DEFAULT);
    }
}
