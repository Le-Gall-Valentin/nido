package com.synapsys.api.authentication.application.port.in;

import com.synapsys.api.authentication.domain.model.AuthTokens;

public interface RefreshTokenUseCase {
    AuthTokens refresh(String rawRefreshToken);
}