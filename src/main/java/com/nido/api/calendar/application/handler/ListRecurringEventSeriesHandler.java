package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ListRecurringEventSeriesUseCase;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListRecurringEventSeriesHandler implements ListRecurringEventSeriesUseCase {

    private final RecurringEventSeriesRepository series;

    public ListRecurringEventSeriesHandler(RecurringEventSeriesRepository series) {
        this.series = series;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringEventSeries> list(SpaceMembership caller) {
        return series.findBySpaceId(caller.spaceId());
    }
}
