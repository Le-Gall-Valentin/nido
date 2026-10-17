package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedError;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalFeedRepository {

    ExternalFeed create(UUID spaceId, String name, URI url, String color, UUID createdBy,
                        String content, String etag, String lastModified, Instant fetchedAt);

    /** Without their calendars: those are read through {@link #loadContent}, only when not in memory already. */
    List<ExternalFeed> findBySpaceId(UUID spaceId);

    Optional<ExternalFeed> findById(UUID feedId);

    long countBySpaceId(UUID spaceId);

    void update(UUID feedId, String name, String color);

    void delete(UUID feedId);

    String loadContent(UUID feedId);

    void recordSuccess(UUID feedId, String content, String etag, String lastModified, Instant at);

    /** Asked, unchanged: the calendar kept stays as it was, and is known current. */
    void recordNotModified(UUID feedId, Instant at);

    /** Asked, failed: the calendar kept stays shown. */
    void recordFailure(UUID feedId, FeedError error, Instant at);

    /** Every calendar last asked for before {@code before}, of every space: what the refresh walks. */
    List<ExternalFeed> findAttemptedBefore(Instant before);
}
