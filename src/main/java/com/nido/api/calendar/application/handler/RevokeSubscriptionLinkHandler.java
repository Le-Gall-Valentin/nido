package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.RevokeSubscriptionLinkUseCase;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class RevokeSubscriptionLinkHandler implements RevokeSubscriptionLinkUseCase {

    private final SubscriptionLinkRepository links;

    public RevokeSubscriptionLinkHandler(SubscriptionLinkRepository links) {
        this.links = links;
    }

    @Override
    @Transactional
    public void revoke(UUID linkId, SpaceMembership caller) {
        links.findById(linkId)
            .filter(link -> link.spaceId().equals(caller.spaceId()) && link.userId().equals(caller.userId()))
            .orElseThrow(CalendarException.SubscriptionLinkNotFound::new);
        links.delete(linkId);
    }
}
