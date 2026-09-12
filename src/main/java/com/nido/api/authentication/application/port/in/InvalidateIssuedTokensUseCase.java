package com.nido.api.authentication.application.port.in;

import java.util.UUID;

/**
 * Called when something makes a user's live access tokens say more than the truth — a role taken
 * away, an account deactivated. Refresh tokens need no equivalent: refreshing re-reads the user
 * from the database, so the next rotation already tells the truth on its own.
 */
public interface InvalidateIssuedTokensUseCase {
    void invalidateFor(UUID userId);
}
