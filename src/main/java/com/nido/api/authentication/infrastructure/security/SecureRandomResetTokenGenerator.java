package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.port.out.ResetTokenGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** 32 bytes from SecureRandom, base64url without padding — the refresh tokens' recipe. */
@Component
public class SecureRandomResetTokenGenerator implements ResetTokenGeneratorPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
