package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.SubscriptionLink;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListSubscriptionLinksUseCase {
    /** The caller's own addresses for their space — nobody sees anyone else's. */
    List<SubscriptionLink> list(SpaceMembership caller);
}
