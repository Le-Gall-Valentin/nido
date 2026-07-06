package com.boilerplate.api.identity.infrastructure.security;

import com.boilerplate.api.authentication.application.port.in.CredentialSetupUseCase;
import com.boilerplate.api.identity.domain.port.out.CredentialSetupPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CredentialSetupAdapter implements CredentialSetupPort {

    private final CredentialSetupUseCase credentialSetupUseCase;

    public CredentialSetupAdapter(CredentialSetupUseCase credentialSetupUseCase) {
        this.credentialSetupUseCase = credentialSetupUseCase;
    }

    @Override
    public void setup(UUID userId, String rawPassword) {
        credentialSetupUseCase.setup(userId, rawPassword);
    }
}