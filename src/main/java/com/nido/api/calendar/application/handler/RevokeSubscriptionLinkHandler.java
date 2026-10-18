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
        // One statement, matched on ids alone: someone else's address, or one of another space, is simply not found.
        if (!links.deleteOwned(linkId, caller.spaceId(), caller.userId())) {
            throw new CalendarException.SubscriptionLinkNotFound();
        }
    }
}
