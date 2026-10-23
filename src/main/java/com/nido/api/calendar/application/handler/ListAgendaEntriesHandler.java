package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ListAgendaEntriesUseCase;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Receives the same {@link CalendarSource} beans as {@link ListCalendarOccurrencesHandler} and keeps the space's own
 * events and the calendars it follows — the agenda of the dashboard is the household's —, so the other sources and
 * their queries never run for this read.
 */
@ApplicationService
public class ListAgendaEntriesHandler implements ListAgendaEntriesUseCase {

    /**
     * What the followed calendars may put in one agenda: ten of them could give twenty thousand entries, and the agenda
     * is a glance. The first ones in the agenda's order are kept.
     */
    static final int MAX_EXTERNAL = 200;

    private final List<CalendarSource> eventSources;

    public ListAgendaEntriesHandler(List<CalendarSource> sources) {
        this.eventSources = sources.stream()
            .filter(source -> source.type() == CalendarSourceType.EVENT || source.type() == CalendarSourceType.EXTERNAL)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarOccurrence> list(SpaceMembership caller, LocalDate from, LocalDate to) {
        OccurrenceWindow.validate(from, to);
        List<CalendarOccurrence> sorted = eventSources.stream()
            .flatMap(source -> source.occurrencesBetween(caller, from, to).stream())
            .sorted(OccurrenceWindow.ORDER)
            .toList();
        List<CalendarOccurrence> listed = new ArrayList<>(sorted.size());
        int externals = 0;
        for (CalendarOccurrence occurrence : sorted) {
            if (occurrence.source() != CalendarSourceType.EXTERNAL || externals++ < MAX_EXTERNAL) {
                listed.add(occurrence);
            }
        }
        return listed;
    }
}
