package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateEventUseCase {
    CalendarEvent update(UpdateEventCommand command, SpaceMembership caller);
}
