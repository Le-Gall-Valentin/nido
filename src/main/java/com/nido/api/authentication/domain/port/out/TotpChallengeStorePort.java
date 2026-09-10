package com.nido.api.authentication.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TotpChallengeStorePort {
    String createChallenge(UUID userId);
    Optional<UUID> resolveChallenge(String challengeId);
    void invalidateChallenge(String challengeId);

    /**
     * Failed verification attempts recorded against this account in the current window, 0 when
     * there are none.
     *
     * <p>Keyed on the account, not on the challenge: a challenge id is minted afresh by every
     * login, so a counter tied to one resets the moment the caller logs in again — which made
     * the five-attempt limit a limit on nothing. Callers check this before verifying a code.
     */
    int failedAttempts(UUID userId);

    /**
     * Records one failed attempt against the account and returns the new count, arming the
     * lockout window if this is the first.
     */
    int recordFailedAttempt(UUID userId);

    /** Wipes the counter. A successful verification clears the slate. */
    void clearFailedAttempts(UUID userId);
}
