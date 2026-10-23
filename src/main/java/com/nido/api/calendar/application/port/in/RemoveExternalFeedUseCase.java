package com.nido.api.calendar.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import java.util.UUID;

public interface RemoveExternalFeedUseCase {
    void remove(UUID feedId, SpaceMembership caller);
}
