package com.nido.api.calendar.infrastructure.security;

import com.nido.api.calendar.domain.port.out.SubscriptionTokenGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** 256 random bits, written in base64url without padding: 43 characters. */
@Component
public class NidoSubscriptionTokenGenerator implements SubscriptionTokenGeneratorPort {

    private static final int BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
