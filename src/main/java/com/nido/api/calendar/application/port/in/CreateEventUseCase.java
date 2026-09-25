package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface CreateEventUseCase {
    CalendarEvent create(CreateEventCommand command, SpaceMembership caller);
}
