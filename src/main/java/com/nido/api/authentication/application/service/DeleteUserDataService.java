package com.nido.api.authentication.application.service;

import com.nido.api.authentication.application.port.in.DeleteUserDataUseCase;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteUserDataService implements DeleteUserDataUseCase {

    private final UserCredentialPort userCredentialPort;
    private final RefreshTokenRevocationPort refreshTokenRevocationPort;
    private final PasswordResetTokenRepository resetTokens;

    public DeleteUserDataService(UserCredentialPort userCredentialPort,
                                 RefreshTokenRevocationPort refreshTokenRevocationPort,
                                 PasswordResetTokenRepository resetTokens) {
        this.userCredentialPort = userCredentialPort;
        this.refreshTokenRevocationPort = refreshTokenRevocationPort;
        this.resetTokens = resetTokens;
    }

    @Override
    @Transactional
    public void delete(UUID userId) {
        resetTokens.deleteAllForUser(userId);
        refreshTokenRevocationPort.deleteAllForUser(userId);
        userCredentialPort.deleteCredential(userId);
    }
}