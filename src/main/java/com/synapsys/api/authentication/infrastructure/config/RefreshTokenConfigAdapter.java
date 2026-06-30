package com.synapsys.api.authentication.infrastructure.config;

import com.synapsys.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenSchedulePort;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenConfigAdapter implements RefreshTokenConfigPort, RefreshTokenSchedulePort {

    private final SynapsysProperties properties;

    public RefreshTokenConfigAdapter(SynapsysProperties properties) {
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