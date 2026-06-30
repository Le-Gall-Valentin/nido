package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.authentication.application.port.in.CredentialSetupUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CredentialSetupAdapterTest {

    @Mock CredentialSetupUseCase credentialSetupUseCase;

    @Test
    void setup_delegatesToCredentialSetupUseCase() {
        CredentialSetupAdapter adapter = new CredentialSetupAdapter(credentialSetupUseCase);
        UUID userId = UUID.randomUUID();

        adapter.setup(userId, "rawPassword");

        verify(credentialSetupUseCase).setup(userId, "rawPassword");
    }
}