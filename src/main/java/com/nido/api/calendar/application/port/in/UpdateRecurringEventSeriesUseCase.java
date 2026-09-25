package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateRecurringEventSeriesUseCase {
    RecurringEventSeries update(UpdateRecurringEventSeriesCommand command, SpaceMembership caller);
}
