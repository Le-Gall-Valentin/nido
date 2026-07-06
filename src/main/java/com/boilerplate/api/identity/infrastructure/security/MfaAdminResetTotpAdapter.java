package com.boilerplate.api.identity.infrastructure.security;

import com.boilerplate.api.identity.domain.port.out.MfaAdminResetTotpPort;
import com.boilerplate.api.mfa.application.port.in.AdminDisableTotpUseCase;
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