package com.nido.api.mfa.infrastructure.security;

import com.nido.api.mfa.domain.port.out.MailCodeGeneratorPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

@Component
public class SecureRandomMailCodeAdapter implements MailCodeGeneratorPort {

    private final RandomGenerator random;

    @Autowired
    public SecureRandomMailCodeAdapter() {
        this(new SecureRandom());
    }

    SecureRandomMailCodeAdapter(RandomGenerator random) {
        this.random = random;
    }

    @Override
    public String newCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
