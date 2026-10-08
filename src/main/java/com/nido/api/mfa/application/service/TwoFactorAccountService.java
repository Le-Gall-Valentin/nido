package com.nido.api.mfa.application.service;

import com.nido.api.mfa.application.method.TwoFactorMethods;
import com.nido.api.mfa.application.port.in.AdminRemoveTwoFactorMethodsUseCase;
import com.nido.api.mfa.application.port.in.DeleteTwoFactorDataUseCase;
import com.nido.api.mfa.application.port.in.GetTwoFactorMethodsUseCase;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** What the other contexts ask mfa about accounts. */
@ApplicationService
public class TwoFactorAccountService
    implements GetTwoFactorMethodsUseCase, AdminRemoveTwoFactorMethodsUseCase, DeleteTwoFactorDataUseCase {

    private final TwoFactorMethodStorePort store;
    private final TwoFactorMethods methods;

    public TwoFactorAccountService(TwoFactorMethodStorePort store, TwoFactorMethods methods) {
        this.store = store;
        this.methods = methods;
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

    @Override
    @Transactional
    public Set<TwoFactorMethod> remove(UUID userId, Set<TwoFactorMethod> requested) {
        Set<TwoFactorMethod> removed = EnumSet.noneOf(TwoFactorMethod.class);
        for (TwoFactorMethod method : requested) {
            if (store.disable(userId, method)) {
                removed.add(method);
            }
            methods.of(method).forgetPending(userId);
        }
        return removed;
    }

    @Override
    @Transactional
    public void deleteUserData(UUID userId) {
        for (TwoFactorMethod method : TwoFactorMethod.values()) {
            methods.of(method).forgetPending(userId);
        }
        store.deleteAll(userId);
    }
}
