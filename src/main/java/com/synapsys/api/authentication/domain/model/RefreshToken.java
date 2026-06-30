package com.synapsys.api.authentication.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
    UUID id,
    UUID userId,
    String tokenHash,
    Instant expiresAt,
    boolean revoked,
    Instant createdAt,
    Instant lastUsedAt
) {
    public boolean isExpired() {
        return isExpired(Clock.systemUTC());
    }

    public boolean isExpired(Clock clock) {
        return expiresAt.isBefore(Instant.now(clock));
    }

    public boolean isRevoked() {
        return revoked;
    }
}