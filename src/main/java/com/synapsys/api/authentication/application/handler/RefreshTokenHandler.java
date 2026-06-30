package com.synapsys.api.authentication.application.handler;

import com.synapsys.api.authentication.application.port.in.RefreshTokenUseCase;
import com.synapsys.api.authentication.domain.model.*;
import com.synapsys.api.authentication.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class RefreshTokenHandler implements RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenHandler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRevocationPort revocationPort;
    private final UserCredentialsPort userCredentialsPort;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final TokenHashPort tokenHashPort;
    private final int refreshTokenExpiryDays;

    public RefreshTokenHandler(RefreshTokenRepository refreshTokenRepository,
                               RefreshTokenRevocationPort revocationPort,
                               UserCredentialsPort userCredentialsPort,
                               AccessTokenPort accessTokenPort,
                               RefreshTokenIssuerPort refreshTokenPort,
                               TokenHashPort tokenHashPort,
                               RefreshTokenConfigPort tokenConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revocationPort = revocationPort;
        this.userCredentialsPort = userCredentialsPort;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.tokenHashPort = tokenHashPort;
        this.refreshTokenExpiryDays = tokenConfig.refreshTokenExpiryDays();
    }

    @Override
    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthenticationException.TokenNotFound();
        }

        String hash = tokenHashPort.hash(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(AuthenticationException.TokenNotFound::new);

        if (token.isRevoked()) {
            log.warn("Revoked token reuse detected for user: {} — revoking all tokens", token.userId());
            try {
                revocationPort.revokeAllForUser(token.userId());
            } catch (Exception e) {
                log.error("Failed to revoke all tokens for user: {}", token.userId(), e);
            }
            throw new AuthenticationException.TokenRevoked();
        }

        if (token.isExpired()) {
            throw new AuthenticationException.TokenExpired();
        }

        UserCredentials creds = userCredentialsPort.findById(token.userId())
            .orElseThrow(AuthenticationException.UserNotFound::new);

        if (!creds.isActive()) {
            log.warn("Refresh token presented for inactive account: {}", creds.id());
            throw new AuthenticationException.UserNotActive();
        }

        if (!revocationPort.tryMarkUsedAndRevoke(token.id())) {
            // Token was concurrently consumed by another request — treat as reuse
            log.warn("Concurrent token reuse detected for user: {}", creds.id());
            throw new AuthenticationException.TokenRevoked();
        }

        log.info("Token rotated for user: {}", creds.id());
        return new AuthTokens(
            accessTokenPort.generate(creds),
            refreshTokenPort.generate(creds, refreshTokenExpiryDays)
        );
    }
}