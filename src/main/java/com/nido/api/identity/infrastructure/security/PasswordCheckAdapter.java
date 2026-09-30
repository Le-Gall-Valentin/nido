package com.nido.api.identity.infrastructure.security;

import com.nido.api.authentication.application.port.in.VerifyPasswordUseCase;
import com.nido.api.identity.domain.port.out.PasswordCheckPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PasswordCheckAdapter implements PasswordCheckPort {

    private final VerifyPasswordUseCase verifyPassword;

    public PasswordCheckAdapter(VerifyPasswordUseCase verifyPassword) {
        this.verifyPassword = verifyPassword;
    }

    @Override
    public boolean matches(UUID userId, String rawPassword) {
        return verifyPassword.verify(userId, rawPassword);
    }
}
