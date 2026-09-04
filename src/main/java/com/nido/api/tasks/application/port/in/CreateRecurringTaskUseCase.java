package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.Task;

public interface CreateRecurringTaskUseCase {
    Task create(CreateRecurringTaskSeriesCommand command, SpaceMembership caller);
}
