package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListRecurringSeriesHandler implements ListRecurringSeriesUseCase {

    private final RecurringTransactionSeriesRepository seriesRepository;

    public ListRecurringSeriesHandler(RecurringTransactionSeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringTransactionSeries> list(SpaceMembership caller) {
        return seriesRepository.findBySpaceId(caller.spaceId());
    }
}
