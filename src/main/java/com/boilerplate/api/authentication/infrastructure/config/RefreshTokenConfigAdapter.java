package com.boilerplate.api.authentication.infrastructure.config;

import com.boilerplate.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.boilerplate.api.authentication.domain.port.out.RefreshTokenSchedulePort;
import com.boilerplate.api.infrastructure.config.BoilerplateProperties;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenConfigAdapter implements RefreshTokenConfigPort, RefreshTokenSchedulePort {

    private final BoilerplateProperties properties;

    public RefreshTokenConfigAdapter(BoilerplateProperties properties) {
        this.properties = properties;
    }

    @Override
    public int refreshTokenExpiryDays() {
        return properties.refreshToken().expiryDays();
    }

    @Override
    public String refreshTokenPurgeCron() {
        return properties.refreshToken().purgeCron();
    }
}