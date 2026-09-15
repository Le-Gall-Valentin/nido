package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface MoveEventUseCase {
    CalendarEvent move(UUID eventId, UUID destinationSpaceId, SpaceMembership caller);
}
