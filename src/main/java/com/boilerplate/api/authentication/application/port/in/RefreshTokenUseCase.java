package com.boilerplate.api.authentication.application.port.in;

import com.boilerplate.api.authentication.domain.model.AuthTokens;

public interface RefreshTokenUseCase {
    AuthTokens refresh(String rawRefreshToken);
}