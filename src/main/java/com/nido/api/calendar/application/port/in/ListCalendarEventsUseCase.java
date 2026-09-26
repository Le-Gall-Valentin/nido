package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;

/**
 * The household's own events between two dates — one-off, detached and projected recurring ones —
 * without the tasks, meals and finance entries {@link ListCalendarOccurrencesUseCase} merges in.
 * Same window rules, same order.
 */
public interface ListCalendarEventsUseCase {
    List<CalendarOccurrence> list(SpaceMembership caller, LocalDate from, LocalDate to);
}
