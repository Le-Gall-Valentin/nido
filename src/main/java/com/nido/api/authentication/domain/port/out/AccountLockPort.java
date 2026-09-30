package com.nido.api.authentication.domain.port.out;

import java.util.UUID;

/**
 * Serialises what must not interleave for one account, until the caller's transaction ends: a session
 * rotating its refresh token while every session is being revoked, two reset requests racing past
 * the cooldown.
 *
 * <p>Taken inside a transaction, and never by one that goes on to call
 * {@link RefreshTokenRevocationPort#revokeAllForUser}: that call takes the lock in a transaction of
 * its own, which would wait on its caller for ever.
 */
public interface AccountLockPort {
    void lockFor(UUID userId);
}
