package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.RefreshExternalFeedUseCase;
import com.nido.api.calendar.application.service.ExternalFeedLookup;
import com.nido.api.calendar.application.service.ExternalFeedRefresher;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

/** Asks a followed calendar again, at a member's request: at most once a minute (ExternalFeedRefresher). */
@ApplicationService
public class RefreshExternalFeedHandler implements RefreshExternalFeedUseCase {

    private final ExternalFeedLookup lookup;
    private final ExternalFeedRefresher refresher;

    public RefreshExternalFeedHandler(ExternalFeedLookup lookup, ExternalFeedRefresher refresher) {
        this.lookup = lookup;
        this.refresher = refresher;
    }

    @Override
    public ExternalFeed refresh(UUID feedId, SpaceMembership caller) {
        caller.ensureCanWrite();
        return refresher.refresh(lookup.ofSpace(feedId, caller));
    }
}
