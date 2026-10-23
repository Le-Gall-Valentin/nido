package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.UpdateExternalFeedUseCase;
import com.nido.api.calendar.application.service.ExternalFeedLookup;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.UpdateExternalFeedCommand;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class UpdateExternalFeedHandler implements UpdateExternalFeedUseCase {

    private final ExternalFeedRepository feeds;
    private final ExternalFeedLookup lookup;

    public UpdateExternalFeedHandler(ExternalFeedRepository feeds, ExternalFeedLookup lookup) {
        this.feeds = feeds;
        this.lookup = lookup;
    }

    @Override
    @Transactional
    public ExternalFeed update(UUID feedId, UpdateExternalFeedCommand command, SpaceMembership caller) {
        caller.ensureCanWrite();
        ExternalFeed feed = lookup.ofSpace(feedId, caller);
        String name = command.name() == null || command.name().isBlank() ? feed.name() : ExternalFeed.fittedName(command.name());
        feeds.update(feed.id(), name, ExternalFeed.colorOrDefault(command.color()));
        return lookup.ofSpace(feedId, caller);
    }
}
