package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ListCalendarEventsUseCase;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Receives the same {@link CalendarSource} beans as {@link ListCalendarOccurrencesHandler} and keeps the space's own
 * events and the calendars it follows — the agenda of the dashboard is the household's —, so the other sources and
 * their queries never run for this read.
 */
@ApplicationService
public class ListCalendarEventsHandler implements ListCalendarEventsUseCase {

    private final List<CalendarSource> eventSources;

    public ListCalendarEventsHandler(List<CalendarSource> sources) {
        this.eventSources = sources.stream()
            .filter(source -> source.type() == CalendarSourceType.EVENT || source.type() == CalendarSourceType.EXTERNAL)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarOccurrence> list(SpaceMembership caller, LocalDate from, LocalDate to) {
        OccurrenceWindow.validate(from, to);
        return eventSources.stream()
            .flatMap(source -> source.occurrencesBetween(caller, from, to).stream())
            .sorted(OccurrenceWindow.ORDER)
            .toList();
    }
}
