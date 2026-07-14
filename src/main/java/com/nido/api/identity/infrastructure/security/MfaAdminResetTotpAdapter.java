package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.port.out.MfaAdminResetTotpPort;
import com.nido.api.mfa.application.port.in.AdminDisableTotpUseCase;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MfaAdminResetTotpAdapter implements MfaAdminResetTotpPort {

    private final AdminDisableTotpUseCase adminDisableTotpUseCase;

    public MfaAdminResetTotpAdapter(AdminDisableTotpUseCase adminDisableTotpUseCase) {
        this.adminDisableTotpUseCase = adminDisableTotpUseCase;
    }

    @Override
    public void disableTotpIfEnabled(UUID userId) {
        adminDisableTotpUseCase.disableIfEnabled(userId);
    }
}