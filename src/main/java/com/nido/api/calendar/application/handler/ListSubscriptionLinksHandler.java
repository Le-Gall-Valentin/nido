package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ListSubscriptionLinksUseCase;
import com.nido.api.calendar.domain.model.SubscriptionLink;
import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListSubscriptionLinksHandler implements ListSubscriptionLinksUseCase {

    private final SubscriptionLinkRepository links;

    public ListSubscriptionLinksHandler(SubscriptionLinkRepository links) {
        this.links = links;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLink> list(SpaceMembership caller) {
        return links.findBySpaceAndUser(caller.spaceId(), caller.userId());
    }
}
