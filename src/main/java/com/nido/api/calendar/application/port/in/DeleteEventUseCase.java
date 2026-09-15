package com.nido.api.calendar.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteEventUseCase {
    void delete(UUID eventId, SpaceMembership caller);
}
