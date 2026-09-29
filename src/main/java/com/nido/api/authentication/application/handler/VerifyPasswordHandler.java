package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.port.in.VerifyPasswordUseCase;
import com.nido.api.authentication.domain.port.out.PasswordVerifierPort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class VerifyPasswordHandler implements VerifyPasswordUseCase {

    private final UserCredentialsPort credentials;
    private final PasswordVerifierPort verifier;

    public VerifyPasswordHandler(UserCredentialsPort credentials, PasswordVerifierPort verifier) {
        this.credentials = credentials;
        this.verifier = verifier;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verify(UUID userId, String rawPassword) {
        return credentials.findById(userId)
            .map(creds -> verifier.matches(rawPassword, creds.passwordHash()))
            .orElse(false);
    }
}
