package com.nido.api.authentication.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A reset link as stored: whose it is and how long it lives — never the token itself. */
public record PasswordResetToken(UUID id, UUID userId, Instant createdAt, Instant expiresAt) {

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
