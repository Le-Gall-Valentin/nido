package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.port.in.RevokePasswordResetLinksUseCase;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class RevokePasswordResetLinksHandler implements RevokePasswordResetLinksUseCase {

    private final PasswordResetTokenRepository tokens;

    public RevokePasswordResetLinksHandler(PasswordResetTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    @Transactional
    public void revokeFor(UUID userId) {
        tokens.deleteAllForUser(userId);
    }
}
