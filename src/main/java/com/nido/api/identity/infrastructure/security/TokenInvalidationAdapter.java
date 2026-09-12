package com.nido.api.identity.infrastructure.security;

import com.nido.api.authentication.application.port.in.InvalidateIssuedTokensUseCase;
import com.nido.api.identity.domain.port.out.TokenInvalidationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TokenInvalidationAdapter implements TokenInvalidationPort {

    private final InvalidateIssuedTokensUseCase invalidateIssuedTokensUseCase;

    public TokenInvalidationAdapter(InvalidateIssuedTokensUseCase invalidateIssuedTokensUseCase) {
        this.invalidateIssuedTokensUseCase = invalidateIssuedTokensUseCase;
    }

    @Override
    public void invalidateIssuedTokens(UUID userId) {
        invalidateIssuedTokensUseCase.invalidateFor(userId);
    }
}
