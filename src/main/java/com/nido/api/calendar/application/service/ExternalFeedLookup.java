package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

/** A followed calendar of the caller's space — any other answers as one that does not exist. */
@ApplicationService
public class ExternalFeedLookup {

    private final ExternalFeedRepository feeds;

    public ExternalFeedLookup(ExternalFeedRepository feeds) {
        this.feeds = feeds;
    }

    public ExternalFeed ofSpace(UUID feedId, SpaceMembership caller) {
        return feeds.findById(feedId).filter(feed -> feed.spaceId().equals(caller.spaceId()))
            .orElseThrow(CalendarException.ExternalFeedNotFound::new);
    }
}
