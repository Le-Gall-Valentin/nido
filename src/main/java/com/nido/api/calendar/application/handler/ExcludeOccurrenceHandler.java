package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ExcludeOccurrenceUseCase;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/** "Cancel just this one." */
@ApplicationService
public class ExcludeOccurrenceHandler implements ExcludeOccurrenceUseCase {

    private final RecurringEventSeriesRepository series;
    private final CalendarEventRepository events;
    private final EventExclusionRepository exclusions;

    public ExcludeOccurrenceHandler(RecurringEventSeriesRepository series, CalendarEventRepository events,
                                    EventExclusionRepository exclusions) {
        this.series = series;
        this.events = events;
        this.exclusions = exclusions;
    }

    @Override
    @Transactional
    public void exclude(UUID seriesId, LocalDate originalDate, SpaceMembership caller) {
        caller.ensureCanWrite();
        RecurringEventSeries found = series.findById(seriesId)
            .orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        if (!found.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.RecurringEventSeriesNotFound();
        }
        if (!EventRecurrenceProjector.slotsBetween(found, originalDate, originalDate).contains(originalDate)) {
            throw new CalendarException.OccurrenceNotInSeries();
        }
        exclusions.exclude(seriesId, originalDate);
        // If the slot had already been edited into its own event, cancelling the occurrence must
        // take that event with it — otherwise the "cancelled" occurrence stays on screen.
        events.findBySeriesAndOriginalDate(seriesId, originalDate)
            .map(CalendarEvent::id)
            .ifPresent(events::delete);
    }
}
