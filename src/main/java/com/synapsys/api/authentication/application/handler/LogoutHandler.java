package com.synapsys.api.authentication.application.handler;

import com.synapsys.api.authentication.application.port.in.LogoutUseCase;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.synapsys.api.authentication.domain.port.out.TokenHashPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class LogoutHandler implements LogoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutHandler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRevocationPort revocationPort;
    private final TokenHashPort tokenHashPort;

    public LogoutHandler(RefreshTokenRepository refreshTokenRepository,
                         RefreshTokenRevocationPort revocationPort,
                         TokenHashPort tokenHashPort) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revocationPort = revocationPort;
        this.tokenHashPort = tokenHashPort;
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        String hash = tokenHashPort.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
            .ifPresent(token -> {
                revocationPort.revoke(token.id());
                log.info("Token revoked on logout for user: {}", token.userId());
            });
    }
}