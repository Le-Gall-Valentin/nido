package com.nido.api.calendar.domain.model;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * A calendar a space follows. Its address is never sent back to anyone — it often holds a secret —: its host is.
 * {@code contentFetchedAt} is when the calendar kept was downloaded; {@code lastAttemptAt} when it was last asked for.
 */
public record ExternalFeed(UUID id, UUID spaceId, String name, URI url, String color, UUID createdBy, Instant createdAt,
                           Instant contentFetchedAt, Instant lastAttemptAt, FeedError lastError, String etag,
                           String lastModified) {

    /** What a calendar wears when none was chosen: the graphite the calendar gives every external source. */
    public static final String DEFAULT_COLOR = "event-graphite";

    /** The colour chosen, or the default: the calendar and the dashboard then always agree on it. */
    public static String colorOrDefault(String color) {
        return color == null ? DEFAULT_COLOR : color;
    }

    public String host() {
        return url.getHost();
    }
}
