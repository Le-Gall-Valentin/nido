package com.synapsys.api.authentication.application.service;

import com.synapsys.api.authentication.application.port.in.DeleteUserDataUseCase;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.synapsys.api.authentication.domain.port.out.UserCredentialPort;
import com.synapsys.api.shared.annotation.ApplicationService;
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