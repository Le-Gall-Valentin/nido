package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.RemoveExternalFeedUseCase;
import com.nido.api.calendar.application.service.ExternalFeedLookup;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class RemoveExternalFeedHandler implements RemoveExternalFeedUseCase {

    private final ExternalFeedRepository feeds;
    private final ExternalFeedLookup lookup;

    public RemoveExternalFeedHandler(ExternalFeedRepository feeds, ExternalFeedLookup lookup) {
        this.feeds = feeds;
        this.lookup = lookup;
    }

    @Override
    @Transactional
    public void remove(UUID feedId, SpaceMembership caller) {
        caller.ensureCanWrite();
        feeds.delete(lookup.ofSpace(feedId, caller).id());
    }
}
