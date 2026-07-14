package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    RefreshToken save(UUID userId, String tokenHash, Instant expiresAt);
}