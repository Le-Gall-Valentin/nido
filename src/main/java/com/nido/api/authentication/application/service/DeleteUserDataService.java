package com.nido.api.authentication.application.service;

import com.nido.api.authentication.application.port.in.DeleteUserDataUseCase;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteUserDataService implements DeleteUserDataUseCase {

    private final UserCredentialPort userCredentialPort;
    private final RefreshTokenRevocationPort refreshTokenRevocationPort;

    public DeleteUserDataService(UserCredentialPort userCredentialPort,
                                 RefreshTokenRevocationPort refreshTokenRevocationPort) {
        this.userCredentialPort = userCredentialPort;
        this.refreshTokenRevocationPort = refreshTokenRevocationPort;
    }

    @Override
    @Transactional
    public void delete(UUID userId) {
        refreshTokenRevocationPort.deleteAllForUser(userId);
        userCredentialPort.deleteCredential(userId);
    }
}