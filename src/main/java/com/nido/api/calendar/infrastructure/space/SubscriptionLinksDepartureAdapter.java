package com.nido.api.calendar.infrastructure.space;

import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.space.domain.port.out.SpaceMemberDepartureHook;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** A member gone from a space takes their subscription addresses with them: they read as that member, who is no more. */
@Component
public class SubscriptionLinksDepartureAdapter implements SpaceMemberDepartureHook {

    private final SubscriptionLinkRepository links;

    public SubscriptionLinksDepartureAdapter(SubscriptionLinkRepository links) {
        this.links = links;
    }

    @Override
    public void departed(UUID spaceId, UUID userId) {
        links.deleteBySpaceAndUser(spaceId, userId);
    }
}
