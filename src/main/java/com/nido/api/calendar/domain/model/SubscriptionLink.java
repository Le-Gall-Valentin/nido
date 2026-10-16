package com.nido.api.calendar.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A secret address one member gave a calendar application, to read one space's calendar without signing in. It is
 * that member's: it reads as they read, stops with their membership, and only they see or revoke it.
 */
public record SubscriptionLink(UUID id, UUID spaceId, UUID userId, String label, Set<CalendarSourceType> sources,
                               boolean onlyMine, Instant createdAt, Instant lastReadAt) {
    public SubscriptionLink {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(label, "label");
        sources = Set.copyOf(sources);
    }
}
