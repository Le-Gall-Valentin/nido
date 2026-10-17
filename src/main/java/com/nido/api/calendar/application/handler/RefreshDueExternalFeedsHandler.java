package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.RefreshDueExternalFeedsUseCase;
import com.nido.api.calendar.application.service.ExternalFeedRefresher;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Reads again the followed calendars due: six hours after their last attempt, plus a delay of each calendar's own —
 * from its id, up to an hour —, so calendars followed together are not all asked at once, with nothing more stored.
 * One calendar failing in an unforeseen way is logged and the others are still read.
 * A tick reads at most {@link #PER_TICK} calendars, in the time it has.
 */
@ApplicationService
public class RefreshDueExternalFeedsHandler implements RefreshDueExternalFeedsUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshDueExternalFeedsHandler.class);
    static final Duration EVERY = Duration.ofHours(6);
    static final long OWN_DELAY_MINUTES = 60;
    /** At most this many calendars per tick, the longest unasked first: the next ticks drain the rest. */
    static final int PER_TICK = 40;
    /** The tick runs every ten minutes: it stops starting new downloads after eight, so two never overlap. */
    static final Duration TIME_BUDGET = Duration.ofMinutes(8);

    private final ExternalFeedRepository feeds;
    private final ExternalFeedRefresher refresher;
    private final Clock clock;

    public RefreshDueExternalFeedsHandler(ExternalFeedRepository feeds, ExternalFeedRefresher refresher, Clock clock) {
        this.feeds = feeds;
        this.refresher = refresher;
        this.clock = clock;
    }

    @Override
    public void refreshDue() {
        Instant start = clock.instant();
        Instant stopStarting = start.plus(TIME_BUDGET);
        for (UUID id : feeds.findDueIds(start.minus(EVERY), PER_TICK)) {
            if (!clock.instant().isBefore(stopStarting)) {
                log.info("Followed calendars: time spent, the rest is left to the next tick");
                return;
            }
            // Each calendar is read back inside the try: one that cannot be (unsealed, parsed) must not stop the others.
            // Only the exception's class is logged: the message of a bad address would hold the address.
            try {
                feeds.findById(id).ifPresent(feed -> refreshIfDue(feed, start));
            } catch (CalendarException.ExternalFeedNotFound removed) {
                log.debug("Followed calendar {} was removed meanwhile", id);
            } catch (RuntimeException e) {
                log.warn("Followed calendar {} could not be refreshed ({})", id, e.getClass().getSimpleName());
            }
        }
    }

    private void refreshIfDue(ExternalFeed feed, Instant now) {
        Duration ownDelay = Duration.ofMinutes(Math.floorMod(feed.id().hashCode(), OWN_DELAY_MINUTES));
        if (feed.lastAttemptAt().plus(EVERY).plus(ownDelay).isAfter(now)) {
            return;
        }
        refresher.refresh(feed);
    }
}
