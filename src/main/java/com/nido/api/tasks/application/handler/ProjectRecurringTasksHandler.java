package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ProjectRecurringTasksUseCase;
import com.nido.api.tasks.domain.model.ProjectedTaskOccurrence;
import com.nido.api.tasks.domain.model.RecurrenceScheduler;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationService
public class ProjectRecurringTasksHandler implements ProjectRecurringTasksUseCase {

    /** Matches the calendar's window cap: no caller can ask for more days than this. */
    private static final int MAX_OCCURRENCES_PER_SERIES = 366;

    private final RecurringTaskSeriesRepository series;

    public ProjectRecurringTasksHandler(RecurringTaskSeriesRepository series) {
        this.series = series;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectedTaskOccurrence> project(SpaceMembership caller, LocalDate from, LocalDate to) {
        List<ProjectedTaskOccurrence> projected = new ArrayList<>();
        for (RecurringTaskSeries s : series.findBySpaceId(caller.spaceId())) {
            // occurrenceCount is the number of the last occurrence generated: creating the series
            // generates occurrence 0 with a count of 0, and the materializer stores the last number it
            // generated. Occurrences 0..occurrenceCount therefore all exist as real tasks and come back
            // from the ordinary task read; the projection starts at the one after, or the most recent
            // task shows twice.
            LocalDate firstUnmaterialized = RecurrenceScheduler.nextDueDate(
                s.anchorDate(), s.intervalType(), s.intervalCount(), s.occurrenceCount() + 1);
            LocalDate windowStart = from.isAfter(firstUnmaterialized) ? from : firstUnmaterialized;
            if (windowStart.isAfter(to)) {
                continue;
            }
            for (LocalDate dueDate : RecurrenceScheduler.occurrencesBetween(
                    s.anchorDate(), s.intervalType(), s.intervalCount(), s.endDate(),
                    windowStart, to, MAX_OCCURRENCES_PER_SERIES)) {
                projected.add(new ProjectedTaskOccurrence(s.id(), s.title(), s.priority(), dueDate));
            }
        }
        return projected;
    }
}
