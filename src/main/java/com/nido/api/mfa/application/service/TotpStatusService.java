package com.nido.api.mfa.application.service;

import com.nido.api.mfa.application.port.in.GetTotpStatusUseCase;
import com.nido.api.mfa.domain.model.UserTotpProfile;
import com.nido.api.mfa.domain.port.out.UserTotpQueryPort;
import com.nido.api.shared.annotation.ApplicationService;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@ApplicationService
public class TotpStatusService implements GetTotpStatusUseCase {

    private final UserTotpQueryPort userTotpQuery;

    public TotpStatusService(UserTotpQueryPort userTotpQuery) {
        this.userTotpQuery = userTotpQuery;
    }

    public boolean isTotpEnabled(UUID userId) {
        return userTotpQuery.findById(userId)
            .map(UserTotpProfile::totpEnabled)
            .orElse(false);
    }

    @Override
    public Set<UUID> findTotpEnabledAmong(Collection<UUID> userIds) {
        return userTotpQuery.findTotpEnabledAmong(userIds);
    }
}