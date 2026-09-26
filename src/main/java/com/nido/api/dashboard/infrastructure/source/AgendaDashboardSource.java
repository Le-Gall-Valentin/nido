package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.calendar.application.port.in.ListCalendarEventsUseCase;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.dashboard.domain.model.AgendaCard;
import com.nido.api.dashboard.domain.model.AgendaEvent;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.tasks.application.port.in.ListTasksDueBetweenUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskOrdering;
import com.nido.api.tasks.domain.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Today: the household's events, today's to-dos for everyone (the caller's first), and a glance at
 * tomorrow. Always present — it is the anchor of the page, even on an empty day.
 */
@Component
public class AgendaDashboardSource implements DashboardSource {

    private static final Comparator<AgendaEvent> ALL_DAY_ORDER =
        Comparator.comparing(AgendaEvent::startDate).thenComparing(AgendaEvent::title);
    private static final Comparator<AgendaEvent> TIMED_ORDER =
        Comparator.comparing(AgendaEvent::startTime, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(AgendaEvent::title);
    /** All-day first, then the earliest start, then the title. */
    private static final Comparator<AgendaEvent> TOMORROW_ORDER =
        Comparator.comparing((AgendaEvent event) -> event.startTime() != null)
            .thenComparing(event -> event.startTime() == null ? LocalTime.MIN : event.startTime())
            .thenComparing(AgendaEvent::title);

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
        LocalDate tomorrow = today.plusDays(1);
        List<CalendarOccurrence> events = listEvents.list(context.caller(), today, tomorrow);

        List<AgendaEvent> allDay = new ArrayList<>();
        List<AgendaEvent> timed = new ArrayList<>();
        for (CalendarOccurrence occurrence : events) {
            if (occurrence.startDate().isAfter(today) || occurrence.endDate().isBefore(today)) {
                continue;
            }
            AgendaEvent event = toEvent(occurrence);
            // An event spanning several days has no single slot on today's timeline.
            if (occurrence.allDay() || !occurrence.startDate().equals(occurrence.endDate())) {
                allDay.add(event);
            } else {
                timed.add(event);
            }
        }
        allDay.sort(ALL_DAY_ORDER);
        timed.sort(TIMED_ORDER);

        AgendaEvent first = events.stream()
            .filter(occurrence -> occurrence.startDate().equals(tomorrow))
            .map(AgendaDashboardSource::toEvent)
            .min(TOMORROW_ORDER)
            .orElse(null);

        List<Task> due = TaskOrdering.sort(listTasksDue.list(context.caller(), today, today).stream()
            .filter(task -> task.status() != TaskStatus.DONE)
            .toList());
        List<TaskItem> dueToday = Stream.concat(
                due.stream().filter(task -> DashboardTaskItems.isMine(task, context)),
                due.stream().filter(task -> !DashboardTaskItems.isMine(task, context)))
            .map(DashboardTaskItems::from)
            .toList();

        return SourceResult.of(new AgendaCard(List.copyOf(allDay), List.copyOf(timed), dueToday, first));
    }

    private static AgendaEvent toEvent(CalendarOccurrence occurrence) {
        return new AgendaEvent(occurrence.sourceId(), occurrence.title(), occurrence.location(), occurrence.color(),
            occurrence.startDate(), occurrence.endDate(), occurrence.startTime(), occurrence.endTime(),
            occurrence.participantIds());
    }
}
