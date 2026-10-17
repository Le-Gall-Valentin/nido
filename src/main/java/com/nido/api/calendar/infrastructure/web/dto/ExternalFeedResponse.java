package com.nido.api.calendar.infrastructure.web.dto;

import com.nido.api.calendar.domain.model.ExternalFeed;

import java.time.Instant;
import java.util.UUID;

/** A followed calendar as anyone in the space sees it: its host, never its address. */
public record ExternalFeedResponse(UUID id, String name, String host, String color, Instant contentFetchedAt,
                                   Instant lastAttemptAt, String lastError) {
    public static ExternalFeedResponse from(ExternalFeed feed) {
        return new ExternalFeedResponse(feed.id(), feed.name(), feed.host(), feed.color(), feed.contentFetchedAt(),
            feed.lastAttemptAt(), feed.lastError() == null ? null : feed.lastError().code());
    }
}
