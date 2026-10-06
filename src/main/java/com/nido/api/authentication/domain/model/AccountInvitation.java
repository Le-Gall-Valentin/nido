package com.nido.api.authentication.domain.model;

import java.time.Instant;
import java.util.UUID;

/** An invitation as stored: whose it is and how long its link lives — never the token itself. */
public record AccountInvitation(UUID userId, Instant createdAt, Instant expiresAt) {

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
