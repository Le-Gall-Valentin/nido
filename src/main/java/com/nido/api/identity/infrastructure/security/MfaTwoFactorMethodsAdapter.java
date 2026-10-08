package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.port.out.TwoFactorMethodsPort;
import com.nido.api.mfa.application.port.in.AdminRemoveTwoFactorMethodsUseCase;
import com.nido.api.mfa.application.port.in.DeleteTwoFactorDataUseCase;
import com.nido.api.mfa.application.port.in.GetTwoFactorMethodsUseCase;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class MfaTwoFactorMethodsAdapter implements TwoFactorMethodsPort {

    private final GetTwoFactorMethodsUseCase methods;
    private final AdminRemoveTwoFactorMethodsUseCase remove;
    private final DeleteTwoFactorDataUseCase delete;

    public MfaTwoFactorMethodsAdapter(GetTwoFactorMethodsUseCase methods, AdminRemoveTwoFactorMethodsUseCase remove,
                                      DeleteTwoFactorDataUseCase delete) {
        this.methods = methods;
        this.remove = remove;
        this.delete = delete;
    }

    @Override
    public Set<TwoFactorMethod> activeMethods(UUID userId) {
        return methods.activeMethods(userId);
    }

    @Override
    public Map<UUID, Set<TwoFactorMethod>> activeMethodsAmong(Collection<UUID> userIds) {
        return methods.activeMethodsAmong(userIds);
    }

    @Override
    public Set<TwoFactorMethod> removeByAdmin(UUID userId, Set<TwoFactorMethod> requested) {
        return remove.remove(userId, requested);
    }

    @Override
    public void deleteAll(UUID userId) {
        delete.deleteUserData(userId);
    }
}
