package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.port.out.TwoFactorMethodsPort;
import com.nido.api.mfa.application.port.in.GetTwoFactorMethodsUseCase;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class MfaTwoFactorMethodsAdapter implements TwoFactorMethodsPort {

    private final GetTwoFactorMethodsUseCase methods;

    public MfaTwoFactorMethodsAdapter(GetTwoFactorMethodsUseCase methods) {
        this.methods = methods;
    }

    @Override
    public Set<TwoFactorMethod> activeMethods(UUID userId) {
        return methods.activeMethods(userId);
    }
}
