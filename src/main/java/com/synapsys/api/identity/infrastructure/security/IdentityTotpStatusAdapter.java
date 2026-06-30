package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.identity.domain.port.out.TotpStatusPort;
import com.synapsys.api.mfa.application.port.in.GetTotpStatusUseCase;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Component
public class IdentityTotpStatusAdapter implements TotpStatusPort {

    private final GetTotpStatusUseCase getTotpStatusUseCase;

    public IdentityTotpStatusAdapter(GetTotpStatusUseCase getTotpStatusUseCase) {
        this.getTotpStatusUseCase = getTotpStatusUseCase;
    }

    @Override
    public boolean isTotpEnabled(UUID userId) {
        return getTotpStatusUseCase.isTotpEnabled(userId);
    }

    @Override
    public Set<UUID> findTotpEnabledAmong(Collection<UUID> userIds) {
        return getTotpStatusUseCase.findTotpEnabledAmong(userIds);
    }
}