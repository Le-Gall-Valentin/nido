package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;

public interface UpdateRecurringTaskSeriesUseCase {
    RecurringTaskSeries update(UpdateRecurringTaskSeriesCommand command, SpaceMembership caller);
}
