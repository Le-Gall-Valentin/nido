package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FollowExternalFeedCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface FollowExternalFeedUseCase {
    ExternalFeed follow(FollowExternalFeedCommand command, SpaceMembership caller);
}
