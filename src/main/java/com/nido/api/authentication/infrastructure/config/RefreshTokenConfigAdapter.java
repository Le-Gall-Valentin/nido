package com.nido.api.authentication.infrastructure.config;

import com.nido.api.authentication.domain.port.out.RefreshTokenSchedulePort;
import com.nido.api.infrastructure.config.NidoProperties;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenConfigAdapter implements RefreshTokenSchedulePort {

    private final NidoProperties properties;

    public RefreshTokenConfigAdapter(NidoProperties properties) {
        this.properties = properties;
    }

    @Override
    public String refreshTokenPurgeCron() {
        return properties.refreshToken().purgeCron();
    }
}