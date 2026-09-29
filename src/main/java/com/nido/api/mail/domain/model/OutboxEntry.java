package com.nido.api.mail.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A mail waiting in the outbox.
 *
 * @param kind     the template id, the only description of the mail that may be logged
 * @param attempts failed delivery attempts so far
 */
public record OutboxEntry(UUID id, String kind, OutgoingMail mail, int attempts, Instant expiresAt) {

    public boolean isExpiredAt(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }
}
