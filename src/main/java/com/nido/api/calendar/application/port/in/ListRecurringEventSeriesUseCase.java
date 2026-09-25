package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListRecurringEventSeriesUseCase {
    List<RecurringEventSeries> list(SpaceMembership caller);
}
