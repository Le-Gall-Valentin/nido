package com.nido.api.calendar.infrastructure.config;

import com.nido.api.calendar.domain.port.out.PublicAddressPort;
import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.PublicUrl;
import com.nido.api.instance.domain.model.SettingKey;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Read from the instance's settings at every call: an address set from the settings page needs no restart. */
@Component
public class PublicAddressAdapter implements PublicAddressPort {

    private final GetEffectiveSettingsQuery settings;

    public PublicAddressAdapter(GetEffectiveSettingsQuery settings) {
        this.settings = settings;
    }

    @Override
    public Optional<String> publicUrl() {
        return settings.current().text(SettingKey.PUBLIC_URL).flatMap(PublicUrl::parse).map(PublicUrl::value);
    }
}
