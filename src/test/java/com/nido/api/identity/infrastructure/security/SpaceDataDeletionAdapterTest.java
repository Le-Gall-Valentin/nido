package com.nido.api.identity.infrastructure.security;

import com.nido.api.space.application.port.in.HandleUserDeletionUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpaceDataDeletionAdapterTest {

    @Mock HandleUserDeletionUseCase handleUserDeletionUseCase;

    @Test
    void deleteSpaceData_delegatesToHandleUserDeletionUseCase() {
        SpaceDataDeletionAdapter adapter = new SpaceDataDeletionAdapter(handleUserDeletionUseCase);
        UUID userId = UUID.randomUUID();

        adapter.deleteSpaceData(userId, "user@example.com");

        verify(handleUserDeletionUseCase).handleUserDeletion(userId, "user@example.com");
    }
}
