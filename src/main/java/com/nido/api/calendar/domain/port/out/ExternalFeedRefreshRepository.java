package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedError;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The calendars followed, as they are asked again: by a member, or on their own every few hours. */
public interface ExternalFeedRefreshRepository {

    Optional<ExternalFeed> findById(UUID feedId);

    /**
     * Marks the calendar asked now, only if it was last asked before {@code lastBefore}, in one statement: of two askings
     * at once, one only is told to go on. False when it was asked since, or is gone.
     */
    boolean claimAttempt(UUID feedId, Instant at, Instant lastBefore);

    /** Marks the calendar asked now, whenever it was last: one that cannot be read back is not asked again at every tick. */
    void markAttempt(UUID feedId, Instant at);

    /** Asked, answered: its calendar and what says how to ask again. Its name and colour are left as they are. */
    void recordSuccess(UUID feedId, String content, String etag, String lastModified, Instant at);

    /** Asked, unchanged: the calendar kept stays as it was, and is known current. */
    void recordNotModified(UUID feedId, Instant at);

    /** Asked, failed: the calendar kept stays shown. */
    void recordFailure(UUID feedId, FeedError error, Instant at);

    /**
     * The ids of the calendars last asked for before {@code before}, of every space, the longest unasked first, at most
     * {@code limit}: only ids, so that one row that cannot be read back does not stop the others.
     */
    List<UUID> findDueIds(Instant before, int limit);
}
