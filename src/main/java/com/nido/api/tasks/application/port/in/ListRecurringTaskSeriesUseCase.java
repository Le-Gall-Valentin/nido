package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;

import java.util.List;

public interface ListRecurringTaskSeriesUseCase {
    List<RecurringTaskSeries> list(SpaceMembership caller);
}
