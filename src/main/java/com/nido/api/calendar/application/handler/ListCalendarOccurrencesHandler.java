package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ListCalendarOccurrencesUseCase;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * The one read that answers "what is happening between these two dates?".
 *
 * <p>Spring injects every {@link CalendarSource} bean, so this handler never names its sources and
 * a sixth one needs no change here. A source that fails is deliberately allowed to fail the whole
 * read: a calendar silently missing a hospital appointment because one adapter threw is worse than
 * an error, since the reader cannot tell "nothing scheduled" from "not shown".
 */
@ApplicationService
public class ListCalendarOccurrencesHandler implements ListCalendarOccurrencesUseCase {

    private final List<CalendarSource> sources;

    public ListCalendarOccurrencesHandler(List<CalendarSource> sources) {
        this.sources = sources;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarOccurrence> list(SpaceMembership caller, LocalDate from, LocalDate to) {
        OccurrenceWindow.validate(from, to);
        return sources.stream()
            .flatMap(source -> source.occurrencesBetween(caller, from, to).stream())
            .sorted(OccurrenceWindow.ORDER)
            .toList();
    }
}
