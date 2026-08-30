package com.nido.api.identity.infrastructure.security;

import com.nido.api.authentication.application.port.in.DeleteUserDataUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CredentialDeletionAdapterTest {

    @Mock DeleteUserDataUseCase deleteUserDataUseCase;

    @Test
    void deleteCredentials_delegatesToDeleteUserDataUseCase() {
        CredentialDeletionAdapter adapter = new CredentialDeletionAdapter(deleteUserDataUseCase);
        UUID userId = UUID.randomUUID();

        adapter.deleteCredentials(userId);

        verify(deleteUserDataUseCase).delete(userId);
    }
}
