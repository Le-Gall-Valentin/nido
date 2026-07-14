package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.port.out.TotpStatusQueryPort;
import com.nido.api.mfa.application.port.in.GetTotpStatusUseCase;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TotpStatusAdapter implements TotpStatusQueryPort {

    private final GetTotpStatusUseCase getTotpStatus;

    public TotpStatusAdapter(GetTotpStatusUseCase getTotpStatus) {
        this.getTotpStatus = getTotpStatus;
    }

    @Override
    public boolean isTotpEnabled(UUID userId) {
        return getTotpStatus.isTotpEnabled(userId);
    }
}