package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CreateSubscriptionLinkCommand;
import com.nido.api.calendar.domain.model.CreatedSubscriptionLink;
import com.nido.api.space.domain.model.SpaceMembership;

public interface CreateSubscriptionLinkUseCase {
    CreatedSubscriptionLink create(CreateSubscriptionLinkCommand command, SpaceMembership caller);
}
