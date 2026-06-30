package com.synapsys.api.authentication.infrastructure.persistence.adapter;

import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenIssuerPort;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.authentication.domain.port.out.TokenHashPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Component
public class RefreshTokenGenerator implements RefreshTokenIssuerPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashPort tokenHashPort;

    public RefreshTokenGenerator(RefreshTokenRepository refreshTokenRepository, TokenHashPort tokenHashPort) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashPort = tokenHashPort;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String generate(UserCredentials user, int expiryDays) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = Instant.now().plusSeconds((long) expiryDays * 86400);
        refreshTokenRepository.save(user.id(), tokenHashPort.hash(raw), expiresAt);
        return raw;
    }
}