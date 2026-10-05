package com.nido.api.authentication.infrastructure.config;

import com.nido.api.authentication.domain.port.out.PublicUrlPort;
import com.nido.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.nido.api.authentication.domain.port.out.SessionSettingsPort;
import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Durations from the instance settings. The cookies' Secure flag: NIDO_COOKIE_SECURE when it is set —
 * the way back in after an https address was saved before the proxy was ready — otherwise whether the
 * public address is https, and true when no address is known, as before this version. And the public address
 * itself, for the links shown to an administrator when mail is off.
 */
@Component
public class SessionSettingsAdapter implements SessionSettingsPort, RefreshTokenConfigPort, PublicUrlPort {

    private final GetEffectiveSettingsQuery settings;
    private final NidoProperties properties;

    public SessionSettingsAdapter(GetEffectiveSettingsQuery settings, NidoProperties properties) {
        this.settings = settings;
        this.properties = properties;
    }

    @Override
    public int accessTokenMinutes() {
        return settings.current().integer(SettingKey.ACCESS_TOKEN_MINUTES);
    }

    @Override
    public int refreshTokenExpiryDays() {
        return settings.current().integer(SettingKey.REFRESH_TOKEN_DAYS);
    }

    @Override
    public boolean secureCookies() {
        Boolean explicit = properties.cookie() == null ? null : properties.cookie().secure();
        if (explicit != null) {
            return explicit;
        }
        return settings.current().text(SettingKey.PUBLIC_URL).map(url -> url.startsWith("https://")).orElse(true);
    }

    @Override
    public Optional<String> publicUrl() {
        return settings.current().text(SettingKey.PUBLIC_URL);
    }
}
