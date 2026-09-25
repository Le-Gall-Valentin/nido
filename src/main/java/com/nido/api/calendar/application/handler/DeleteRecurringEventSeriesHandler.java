package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.DeleteRecurringEventSeriesUseCase;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteRecurringEventSeriesHandler implements DeleteRecurringEventSeriesUseCase {

    private final RecurringEventSeriesRepository series;

    public DeleteRecurringEventSeriesHandler(RecurringEventSeriesRepository series) {
        this.series = series;
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
        // Exclusions, participants and detached instances go with it, by ON DELETE CASCADE —
        // asserted in CalendarSchemaIT rather than re-deleted by hand here.
        series.delete(seriesId);
    }
}
