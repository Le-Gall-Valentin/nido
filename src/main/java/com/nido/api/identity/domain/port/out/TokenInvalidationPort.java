package com.nido.api.identity.domain.port.out;

import java.util.UUID;

/**
 * Identity decides that a user's rights changed; authentication owns what that does to the tokens
 * already in the wild. This port is the seam between the two, so identity never has to know that
 * tokens exist at all.
 */
public interface TokenInvalidationPort {
    void invalidateIssuedTokens(UUID userId);
}
