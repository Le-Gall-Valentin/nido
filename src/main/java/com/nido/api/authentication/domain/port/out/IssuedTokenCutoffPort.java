package com.nido.api.authentication.domain.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Remembers that everything issued to a user before a given moment must stop being accepted.
 *
 * <p>An access token is signed and self-contained: it cannot be un-issued. The only way to cut a
 * live one short is to keep a note of the cut-off and consult it. That note is the whole of the
 * state this adds — one entry per user whose rights actually changed, and only for as long as a
 * token issued before the change could still be alive.
 *
 * <p>The comparison is against the token's own {@code iat}, which every token already carries, so
 * tokens minted before this existed are covered too: nothing has to be re-issued for the mechanism
 * to start working.
 */
public interface IssuedTokenCutoffPort {

    /**
     * Refuses every token issued to this user up to now. Implementations must forget the cut-off
     * once no token predating it can still be valid — a user is not remembered forever for having
     * been demoted once.
     */
    void cutOffNow(UUID userId);

    /** Empty when nothing was ever cut off for this user, or when the cut-off has aged out. */
    Optional<Instant> cutoffFor(UUID userId);
}
