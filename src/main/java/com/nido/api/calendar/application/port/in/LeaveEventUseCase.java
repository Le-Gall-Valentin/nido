package com.nido.api.calendar.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface LeaveEventUseCase {
    void leave(UUID eventId, SpaceMembership caller);
}
