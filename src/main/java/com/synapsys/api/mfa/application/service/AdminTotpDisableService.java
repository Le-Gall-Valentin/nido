package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.application.port.in.AdminDisableTotpUseCase;
import com.synapsys.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import com.synapsys.api.shared.annotation.ApplicationService;
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