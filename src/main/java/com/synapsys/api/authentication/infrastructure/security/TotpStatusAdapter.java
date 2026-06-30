package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.authentication.domain.port.out.TotpStatusQueryPort;
import com.synapsys.api.mfa.application.port.in.GetTotpStatusUseCase;
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