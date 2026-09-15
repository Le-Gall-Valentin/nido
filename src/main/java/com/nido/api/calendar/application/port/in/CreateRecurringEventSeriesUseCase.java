package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.space.domain.model.SpaceMembership;

public interface CreateRecurringEventSeriesUseCase {
    RecurringEventSeries create(CreateRecurringEventSeriesCommand command, SpaceMembership caller);
}
