package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.authentication.application.port.in.CredentialSetupUseCase;
import com.synapsys.api.identity.domain.port.out.CredentialSetupPort;
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