package com.nido.api.identity.infrastructure.security;

import com.nido.api.authentication.application.port.in.DeleteUserDataUseCase;
import com.nido.api.identity.domain.port.out.CredentialDeletionPort;
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