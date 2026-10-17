package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.RefreshDueExternalFeedsUseCase;
import com.nido.api.calendar.application.service.ExternalFeedRefresher;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Reads again the followed calendars due: six hours after their last attempt, plus a delay of each calendar's own —
 * from its id, up to an hour —, so calendars followed together are not all asked at once, with nothing more stored.
 * One calendar failing in an unforeseen way is logged and the others are still read.
 */
@ApplicationService
public class RefreshDueExternalFeedsHandler implements RefreshDueExternalFeedsUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshDueExternalFeedsHandler.class);
    static final Duration EVERY = Duration.ofHours(6);
    static final long OWN_DELAY_MINUTES = 60;

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
        Instant now = clock.instant();
        for (ExternalFeed feed : feeds.findAttemptedBefore(now.minus(EVERY))) {
            Duration ownDelay = Duration.ofMinutes(Math.floorMod(feed.id().hashCode(), OWN_DELAY_MINUTES));
            if (feed.lastAttemptAt().plus(EVERY).plus(ownDelay).isAfter(now)) {
                continue;
            }
            try {
                refresher.refresh(feed);
            } catch (RuntimeException e) {
                log.warn("Followed calendar {} of space {} could not be refreshed", feed.id(), feed.spaceId(), e);
            }
        }
    }
}
