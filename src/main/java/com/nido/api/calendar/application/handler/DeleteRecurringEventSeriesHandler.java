package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.DeleteRecurringEventSeriesUseCase;
import com.nido.api.calendar.application.service.SeriesExceptionsMover;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Deleting a series never erases its past either: one that has begun and is not over stops the day
 * before, and everything still to come goes — the occurrences edited on their own included. A series
 * not begun yet, or already over, is deleted whole, which is also how a finished one is erased.
 */
@ApplicationService
public class DeleteRecurringEventSeriesHandler implements DeleteRecurringEventSeriesUseCase {

    private final RecurringEventSeriesRepository series;
    private final GetSpaceTodayUseCase spaceToday;
    private final SeriesExceptionsMover exceptions;

    public DeleteRecurringEventSeriesHandler(RecurringEventSeriesRepository series, GetSpaceTodayUseCase spaceToday,
                                             SeriesExceptionsMover exceptions) {
        this.series = series;
        this.spaceToday = spaceToday;
        this.exceptions = exceptions;
    }

    @Override
    @Transactional
    public void delete(UUID seriesId, SpaceMembership caller) {
        caller.ensureCanWrite();
        RecurringEventSeries existing = series.findById(seriesId)
            .orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.RecurringEventSeriesNotFound();
        }
        LocalDate today = spaceToday.today(caller.spaceId());
        if (EventRecurrenceProjector.runsAcross(existing, today)) {
            series.endOn(seriesId, today.minusDays(1));
            exceptions.dropFrom(seriesId, today);
            return;
        }
        // Exclusions, participants and detached instances go with it, by ON DELETE CASCADE —
        // asserted in CalendarSchemaIT rather than re-deleted by hand here.
        series.delete(seriesId);
    }
}
