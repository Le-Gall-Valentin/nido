package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.space.domain.model.SpaceMembership;
import java.util.UUID;

public interface UpdateExternalFeedUseCase {
    ExternalFeed update(UUID feedId, String name, String color, SpaceMembership caller);
}
