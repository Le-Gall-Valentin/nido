package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ListExternalFeedsUseCase;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListExternalFeedsHandler implements ListExternalFeedsUseCase {

    private final ExternalFeedRepository feeds;

    public ListExternalFeedsHandler(ExternalFeedRepository feeds) {
        this.feeds = feeds;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalFeed> list(SpaceMembership caller) {
        return feeds.findBySpaceId(caller.spaceId());
    }
}
