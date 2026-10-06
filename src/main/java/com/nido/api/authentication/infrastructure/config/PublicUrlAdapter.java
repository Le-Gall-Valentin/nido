package com.nido.api.authentication.infrastructure.config;

import com.nido.api.authentication.domain.port.out.PublicUrlPort;
import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** The public address from the instance settings, read at each call: it changes without a restart. */
@Component
public class PublicUrlAdapter implements PublicUrlPort {

    private final GetEffectiveSettingsQuery settings;

    public PublicUrlAdapter(GetEffectiveSettingsQuery settings) {
        this.settings = settings;
    }

    @Override
    public Optional<String> publicUrl() {
        return settings.current().text(SettingKey.PUBLIC_URL);
    }
}
