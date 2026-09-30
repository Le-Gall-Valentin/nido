package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {

    void save(UUID userId, String tokenHash, Instant createdAt, Instant expiresAt);

    Optional<PasswordResetToken> findByHash(String tokenHash);

    /**
     * Removes the token and returns it, locking the row first: of two confirmations racing with the
     * same link, one gets the token and the other finds nothing.
     */
    Optional<PasswordResetToken> consumeByHash(String tokenHash);

    Optional<Instant> latestIssuedAt(UUID userId);

    void deleteAllForUser(UUID userId);

    int deleteExpired(Instant now);
}
