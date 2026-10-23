package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;

/**
 * What an agenda shows between two dates: the household's own events — one-off, detached and projected recurring ones —
 * and the calendars the space follows, at most 200 entries of those; without the tasks, meals and finance entries
 * {@link ListCalendarOccurrencesUseCase} merges in. Same window rules, same order.
 */
public interface ListAgendaEntriesUseCase {
    List<CalendarOccurrence> list(SpaceMembership caller, LocalDate from, LocalDate to);
}
