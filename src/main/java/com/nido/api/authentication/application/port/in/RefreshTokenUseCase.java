package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.domain.model.AuthTokens;

public interface RefreshTokenUseCase {
    AuthTokens refresh(String rawRefreshToken);
}