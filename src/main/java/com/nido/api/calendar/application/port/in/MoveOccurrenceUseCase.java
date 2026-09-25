package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.UUID;

public interface MoveOccurrenceUseCase {
    CalendarEvent move(UUID seriesId, LocalDate originalDate, UUID destinationSpaceId, SpaceMembership caller);
}
