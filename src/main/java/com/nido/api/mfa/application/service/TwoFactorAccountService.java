package com.nido.api.mfa.application.service;

import com.nido.api.mfa.application.port.in.GetTwoFactorMethodsUseCase;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** What the other contexts ask mfa about accounts. */
@ApplicationService
public class TwoFactorAccountService implements GetTwoFactorMethodsUseCase {

    private final TwoFactorMethodStorePort store;

    public TwoFactorAccountService(TwoFactorMethodStorePort store) {
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<TwoFactorMethod> activeMethods(UUID userId) {
        return store.activeMethods(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Set<TwoFactorMethod>> activeMethodsAmong(Collection<UUID> userIds) {
        return store.activeMethodsAmong(userIds);
    }
}
