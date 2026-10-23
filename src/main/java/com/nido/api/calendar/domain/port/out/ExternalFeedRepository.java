package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.ExternalFeed;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The calendars a space follows, as its members manage them. */
public interface ExternalFeedRepository {

    /**
     * Follows a calendar, unless its space follows {@code most} already: counted and written in one go, so that two
     * follows at once never pass the limit together. Empty when the space follows that many.
     */
    Optional<ExternalFeed> createUnlessFollowing(int most, UUID spaceId, String name, URI url, String color, UUID createdBy,
                                                 String content, String etag, String lastModified, Instant fetchedAt);

    /** Without their calendars: those are read through ExternalFeedContentRepository, only when not in memory already. */
    List<ExternalFeed> findBySpaceId(UUID spaceId);

    Optional<ExternalFeed> findById(UUID feedId);

    long countBySpaceId(UUID spaceId);

    /** Changes its name and colour only: a refresh writing at the same time keeps what it writes. */
    void update(UUID feedId, String name, String color);

    void delete(UUID feedId);
}
