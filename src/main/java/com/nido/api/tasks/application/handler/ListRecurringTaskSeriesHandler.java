package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ListRecurringTaskSeriesUseCase;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;

import java.util.List;

@ApplicationService
public class ListRecurringTaskSeriesHandler implements ListRecurringTaskSeriesUseCase {

    private final RecurringTaskSeriesRepository seriesRepository;

    public ListRecurringTaskSeriesHandler(RecurringTaskSeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    public List<RecurringTaskSeries> list(SpaceMembership caller) {
        return seriesRepository.findBySpaceId(caller.spaceId());
    }
}
