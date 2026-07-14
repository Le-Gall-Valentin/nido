package com.nido.api.authentication.application.service;

import com.nido.api.authentication.application.port.in.CredentialSetupUseCase;
import com.nido.api.authentication.domain.port.out.PasswordHasherPort;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import com.nido.api.shared.annotation.ApplicationService;
import java.util.UUID;

@ApplicationService
public class CredentialSetupService implements CredentialSetupUseCase {

    private final PasswordHasherPort passwordHasher;
    private final UserCredentialPort userCredentialPort;

    public CredentialSetupService(PasswordHasherPort passwordHasher,
                                  UserCredentialPort userCredentialPort) {
        this.passwordHasher = passwordHasher;
        this.userCredentialPort = userCredentialPort;
    }

    @Override
    public void setup(UUID userId, String rawPassword) {
        String hash = passwordHasher.hash(rawPassword);
        userCredentialPort.saveCredential(userId, hash);
    }
}