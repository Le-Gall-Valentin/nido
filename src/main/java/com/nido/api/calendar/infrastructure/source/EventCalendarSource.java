package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The calendar's own events: the stored rows, plus every series expanded over the window.
 *
 * <p>The two halves must not overlap. A detached instance is a stored row <em>and</em> occupies a
 * slot of its series, so the projector is told which slots are taken and skips them — otherwise
 * the same event would appear twice, once at its new date and once at its original one.
 */
@Component
public class EventCalendarSource implements CalendarSource {

    private final CalendarEventRepository events;
    private final RecurringEventSeriesRepository series;
    private final EventExclusionRepository exclusions;

    public EventCalendarSource(CalendarEventRepository events, RecurringEventSeriesRepository series,
                               EventExclusionRepository exclusions) {
        this.events = events;
        this.series = series;
        this.exclusions = exclusions;
    }

    @Override
    public CalendarSourceType type() {
        return CalendarSourceType.EVENT;
    }

    @Override
    public List<CalendarOccurrence> occurrencesBetween(SpaceMembership caller, LocalDate from, LocalDate to) {
        List<CalendarOccurrence> produced = new ArrayList<>();
        for (CalendarEvent event : events.findBySpaceIdOverlapping(caller.spaceId(), from, to)) {
            produced.add(new CalendarOccurrence(
                CalendarSourceType.EVENT, event.id().toString(),
                event.recurringSeriesId(), event.recurringOriginalDate(), true,
                event.title(), event.description(), event.location(), event.allDay(),
                event.startDate(), event.startTime(), event.endDate(), event.endTime(),
                event.color(), event.participantIds()));
        }
        for (RecurringEventSeries s : series.findBySpaceId(caller.spaceId())) {
            // Looked up as far back as the projector scans: an occurrence lasting several days that
            // began before the window is still projected, so its cancellation or its detached
            // instance must be found too.
            LocalDate scanFrom = EventRecurrenceProjector.scanFrom(s, from);
            produced.addAll(EventRecurrenceProjector.project(
                s,
                exclusions.findSlots(s.id(), scanFrom, to),
                events.findDetachedSlots(s.id(), scanFrom, to),
                from, to));
        }
        return produced;
    }
}
