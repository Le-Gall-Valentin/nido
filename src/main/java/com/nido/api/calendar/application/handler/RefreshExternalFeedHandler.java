package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.RefreshExternalFeedUseCase;
import com.nido.api.calendar.application.service.ExternalFeedLookup;
import com.nido.api.calendar.application.service.ExternalFeedRefresher;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@ApplicationService
public class RefreshExternalFeedHandler implements RefreshExternalFeedUseCase {

    private static final Duration AT_MOST_EVERY = Duration.ofMinutes(1);

    private final ExternalFeedLookup lookup;
    private final ExternalFeedRefresher refresher;
    private final Clock clock;

    public RefreshExternalFeedHandler(ExternalFeedLookup lookup, ExternalFeedRefresher refresher, Clock clock) {
        this.lookup = lookup;
        this.refresher = refresher;
        this.clock = clock;
    }

    @Override
    public ExternalFeed refresh(UUID feedId, SpaceMembership caller) {
        caller.ensureCanWrite();
        ExternalFeed feed = lookup.ofSpace(feedId, caller);
        if (feed.lastAttemptAt().isAfter(clock.instant().minus(AT_MOST_EVERY))) {
            throw new CalendarException.RefreshTooSoon();
        }
        return refresher.refresh(feed);
    }
}
