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
            // Every date up to the cursor is already a real transaction, which the calendar lists on
            // its own; projecting those too showed each past occurrence, and today's, twice. Only
            // what the series has not accounted for yet is projected — the cursor, not "today",
            // so a date that is due but not materialized yet still shows, once.
            LocalDate firstUnaccounted = series.lastMaterializedDate() == null
                ? series.anchorDate() : series.lastMaterializedDate().plusDays(1);
            LocalDate windowStart = from.isAfter(firstUnaccounted) ? from : firstUnaccounted;
            if (windowStart.isAfter(to)) {
                continue;
            }
            for (LocalDate date : RecurrenceProjector.occurrencesBetween(
                    series.anchorDate(), series.intervalType(), series.intervalCount(), series.endDate(),
                    windowStart, to, MAX_PROJECTED_OCCURRENCES_PER_SERIES)) {
                upcoming.add(new ProjectedOccurrence(
                    series.id(), series.label(), series.amount(), series.type(), date));
            }
        }
        return upcoming;
    }
}
