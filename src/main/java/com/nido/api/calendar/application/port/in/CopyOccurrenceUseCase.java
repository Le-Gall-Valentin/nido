package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.UUID;

public interface CopyOccurrenceUseCase {
    CalendarEvent copy(UUID seriesId, LocalDate originalDate, UUID destinationSpaceId, SpaceMembership caller);
}
