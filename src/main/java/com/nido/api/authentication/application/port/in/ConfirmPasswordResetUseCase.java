package com.nido.api.authentication.application.port.in;

public interface ConfirmPasswordResetUseCase {
    /** @throws com.nido.api.authentication.domain.model.AuthenticationException.InvalidResetToken */
    void confirm(String token, String newPassword);
}
