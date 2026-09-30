package com.nido.api.identity.infrastructure.security;

import com.nido.api.authentication.application.port.in.RevokePasswordResetLinksUseCase;
import com.nido.api.identity.domain.port.out.AccountRecoveryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** The reset links live in authentication, which owns everything about getting back into an account. */
@Component
public class AccountRecoveryAdapter implements AccountRecoveryPort {

    private final RevokePasswordResetLinksUseCase revokeResetLinks;

    public AccountRecoveryAdapter(RevokePasswordResetLinksUseCase revokeResetLinks) {
        this.revokeResetLinks = revokeResetLinks;
    }

    @Override
    public void forgetResetLinks(UUID userId) {
        revokeResetLinks.revokeFor(userId);
    }
}
