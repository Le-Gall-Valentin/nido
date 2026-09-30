package com.nido.api.authentication.application.port.in;

public interface CheckPasswordResetTokenUseCase {
    /** @throws com.nido.api.authentication.domain.model.AuthenticationException.InvalidResetToken */
    void check(String token);
}
