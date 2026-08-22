package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.port.out.SpaceDataDeletionPort;
import com.nido.api.space.application.port.in.HandleUserDeletionUseCase;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SpaceDataDeletionAdapter implements SpaceDataDeletionPort {

    private final HandleUserDeletionUseCase handleUserDeletionUseCase;

    public SpaceDataDeletionAdapter(HandleUserDeletionUseCase handleUserDeletionUseCase) {
        this.handleUserDeletionUseCase = handleUserDeletionUseCase;
    }

    @Override
    public void deleteSpaceData(UUID userId) {
        handleUserDeletionUseCase.handleUserDeletion(userId);
    }
}
