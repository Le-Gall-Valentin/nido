package com.boilerplate.api.identity.infrastructure.security;

import com.boilerplate.api.authentication.application.port.in.DeleteUserDataUseCase;
import com.boilerplate.api.identity.domain.port.out.CredentialDeletionPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CredentialDeletionAdapter implements CredentialDeletionPort {

    private final DeleteUserDataUseCase deleteUserDataUseCase;

    public CredentialDeletionAdapter(DeleteUserDataUseCase deleteUserDataUseCase) {
        this.deleteUserDataUseCase = deleteUserDataUseCase;
    }

    @Override
    public void deleteCredentials(UUID userId) {
        deleteUserDataUseCase.delete(userId);
    }
}