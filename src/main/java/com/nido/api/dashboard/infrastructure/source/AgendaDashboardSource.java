package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.calendar.application.port.in.ListCalendarEventsUseCase;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.dashboard.domain.model.AgendaDay;
import com.nido.api.dashboard.domain.model.AgendaEvent;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.tasks.application.port.in.ListTasksDueBetweenUseCase;
import com.nido.api.tasks.domain.model.TaskOrdering;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Reads today's and tomorrow's events from the calendar and today's tasks from the tasks module, and
 * lets {@link AgendaDay} decide the card.
 */
@Component
public class AgendaDashboardSource implements DashboardSource {

    private final ListCalendarEventsUseCase listEvents;
    private final ListTasksDueBetweenUseCase listTasksDue;

    public AgendaDashboardSource(ListCalendarEventsUseCase listEvents, ListTasksDueBetweenUseCase listTasksDue) {
        this.listEvents = listEvents;
        this.listTasksDue = listTasksDue;
    }

    @Override
    public CardKind kind() {
        return CardKind.AGENDA;
    }

    @Override
    public SourceResult read(DashboardContext context) {
        LocalDate today = context.today();
        List<AgendaEvent> events = listEvents.list(context.caller(), today, today.plusDays(1)).stream()
            .map(AgendaDashboardSource::toEvent)
            .toList();
        // This read has no order of its own; the tasks board's order is the tasks module's rule.
        List<TaskItem> dueToday = TaskOrdering.sort(listTasksDue.list(context.caller(), today, today)).stream()
            .map(DashboardTaskItems::from)
            .toList();
        return SourceResult.of(AgendaDay.of(events, dueToday, context));
    }

    /** An all-day occurrence goes without times, whatever it carries: that is how AgendaEvent tells it apart. */
    private static AgendaEvent toEvent(CalendarOccurrence occurrence) {
        boolean allDay = occurrence.allDay();
        return new AgendaEvent(occurrence.sourceId(), occurrence.title(), occurrence.location(), occurrence.color(),
            occurrence.startDate(), occurrence.endDate(),
            allDay ? null : occurrence.startTime(), allDay ? null : occurrence.endTime(),
            occurrence.participantIds());
    }
}
