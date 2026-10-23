package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.space.domain.model.SpaceMembership;
import java.util.UUID;

public interface RefreshExternalFeedUseCase {
    ExternalFeed refresh(UUID feedId, SpaceMembership caller);
}
