package com.boilerplate.api.mfa.application.service;

import com.boilerplate.api.mfa.application.port.in.AdminDisableTotpUseCase;
import com.boilerplate.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.boilerplate.api.mfa.domain.port.out.UserTotpQueryPort;
import com.boilerplate.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@ApplicationService
public class AdminTotpDisableService implements AdminDisableTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final UserTotpLifecyclePort userTotpLifecyclePort;

    public AdminTotpDisableService(UserTotpQueryPort userTotpQuery, UserTotpLifecyclePort userTotpLifecyclePort) {
        this.userTotpQuery = userTotpQuery;
        this.userTotpLifecyclePort = userTotpLifecyclePort;
    }

    @Transactional
    public void disableIfEnabled(UUID userId) {
        userTotpQuery.findById(userId).ifPresent(profile -> {
            if (profile.totpEnabled()) userTotpLifecyclePort.disableTotp(userId);
        });
    }
}