package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ListCalendarOccurrencesUseCase;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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

    /** All-day entries sort before timed ones on the same day, then by time, then by title. */
    private static final Comparator<CalendarOccurrence> ORDER =
        Comparator.comparing(CalendarOccurrence::startDate)
            .thenComparing(o -> o.startTime() == null ? LocalTime.MIN : o.startTime())
            .thenComparing(CalendarOccurrence::title);

    private final List<CalendarSource> sources;

    public ListCalendarOccurrencesHandler(List<CalendarSource> sources) {
        this.sources = sources;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarOccurrence> list(SpaceMembership caller, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new CalendarException.ReversedWindow();
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > EventRecurrenceProjector.MAX_WINDOW_DAYS) {
            throw new CalendarException.WindowTooLarge((int) Math.min(days, Integer.MAX_VALUE),
                EventRecurrenceProjector.MAX_WINDOW_DAYS);
        }
        return sources.stream()
            .flatMap(source -> source.occurrencesBetween(caller, from, to).stream())
            .sorted(ORDER)
            .toList();
    }
}
