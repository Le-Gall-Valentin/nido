package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.AccountInvitation;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AccountInvitationRepository {

    /** The account's invitation from now on: any previous one, and its link, is gone. */
    void save(UUID userId, String tokenHash, Instant createdAt, Instant expiresAt);

    Optional<AccountInvitation> findByHash(String tokenHash);

    /**
     * Removes the invitation and returns it, locking the row first: of two acceptances racing with the same
     * link, one gets it and the other finds nothing.
     */
    Optional<AccountInvitation> consumeByHash(String tokenHash);

    Optional<AccountInvitation> findByUserId(UUID userId);

    /** The invitations among these accounts, by account; an account without one is absent. */
    Map<UUID, AccountInvitation> findByUserIds(Collection<UUID> userIds);

    void deleteForUser(UUID userId);
}
