package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ProjectRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.RecurrenceProjector;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationService
public class ProjectRecurringSeriesHandler implements ProjectRecurringSeriesUseCase {

    /** Matches the calendar's window cap: no caller can ask for more days than this. */
    private static final int MAX_PROJECTED_OCCURRENCES_PER_SERIES = 366;

    private final RecurringTransactionSeriesRepository seriesRepository;

    public ProjectRecurringSeriesHandler(RecurringTransactionSeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectedOccurrence> project(SpaceMembership caller, LocalDate from, LocalDate to) {
        List<ProjectedOccurrence> upcoming = new ArrayList<>();
        for (RecurringTransactionSeries series : seriesRepository.findActiveBySpaceId(caller.spaceId(), from)) {
            for (LocalDate date : RecurrenceProjector.occurrencesBetween(
                    series.anchorDate(), series.intervalType(), series.intervalCount(), series.endDate(),
                    from, to, MAX_PROJECTED_OCCURRENCES_PER_SERIES)) {
                upcoming.add(new ProjectedOccurrence(
                    series.id(), series.label(), series.amount(), series.type(), date));
            }
        }
        return upcoming;
    }
}
