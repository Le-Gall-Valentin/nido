package com.synapsys.api.authentication.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TotpChallengeStorePort {
    String createChallenge(UUID userId);
    Optional<UUID> resolveChallenge(String challengeId);
    void invalidateChallenge(String challengeId);

    /**
     * Increments the failed-attempt counter for a challenge and returns the new count.
     * TTL is set only on the first increment (matches challenge TTL).
     * Callers must invalidate the challenge when the count reaches the configured maximum.
     */
    int incrementFailedAttempts(String challengeId);
}