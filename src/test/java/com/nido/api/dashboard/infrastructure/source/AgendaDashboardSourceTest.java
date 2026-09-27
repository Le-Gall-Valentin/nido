package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.calendar.application.port.in.ListCalendarEventsUseCase;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.dashboard.domain.model.AgendaCard;
import com.nido.api.dashboard.domain.model.AgendaEvent;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.tasks.application.port.in.ListTasksDueBetweenUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgendaDashboardSourceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private final UUID spaceId = UUID.randomUUID();
    private final UUID me = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, me, SpaceRole.MEMBER, Instant.now());
    private final ListCalendarEventsUseCase listEvents = mock(ListCalendarEventsUseCase.class);
    private final ListTasksDueBetweenUseCase listTasksDue = mock(ListTasksDueBetweenUseCase.class);
    private int created;

    @BeforeEach
    void noTasksByDefault() {
        when(listTasksDue.list(caller, TODAY, TODAY)).thenReturn(List.of());
    }

    @Test
    void readsTodayAndTomorrowFromTheCalendarAndLetsTheDayDecide() {
        // The rules are AgendaDay's (see AgendaDayTest); this only checks they get the right two days.
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(
            timed("Marché", TODAY, LocalTime.of(10, 30), TODAY, LocalTime.of(12, 0)),
            allDay("Fête du quartier", TOMORROW, TOMORROW)));

        AgendaCard card = read();

        assertThat(card.timed()).extracting(AgendaEvent::title).containsExactly("Marché");
        assertThat(card.tomorrow().title()).isEqualTo("Fête du quartier");
    }

    @Test
    void todaysToDosComeInTheBoardOrder() {
        // The calendar's read of due tasks has no order; the tasks board's is priority first.
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of());
        when(listTasksDue.list(caller, TODAY, TODAY)).thenReturn(List.of(
            task("Arroser les plantes", TaskPriority.LOW), task("Appeler le médecin", TaskPriority.HIGH)));

        assertThat(read().dueToday()).extracting(TaskItem::title).containsExactly("Appeler le médecin", "Arroser les plantes");
    }

    @Test
    void anEventCarriesItsCalendarFields() {
        UUID participant = UUID.randomUUID();
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(new CalendarOccurrence(
            CalendarSourceType.EVENT, "evt-1", null, null, true, "Pédiatre", null, "Cabinet", false,
            TODAY, LocalTime.of(16, 0), TODAY, LocalTime.of(16, 30), "indigo", List.of(participant))));

        assertThat(read().timed()).containsExactly(new AgendaEvent("evt-1", "Pédiatre", "Cabinet", "indigo",
            TODAY, TODAY, LocalTime.of(16, 0), LocalTime.of(16, 30), List.of(participant)));
    }

    @Test
    void anAllDayOccurrenceReachesTheCardWithoutTimes() {
        // AgendaEvent#allDay reads "no start time": whatever times the calendar carries, all day means none.
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(new CalendarOccurrence(
            CalendarSourceType.EVENT, "evt-2", null, null, true, "Anniversaire", null, null, true,
            TODAY, LocalTime.of(0, 0), TODAY, LocalTime.of(23, 59), null, List.of())));

        AgendaEvent event = read().allDay().getFirst();

        assertThat(event.startTime()).isNull();
        assertThat(event.endTime()).isNull();
        assertThat(event.allDay()).isTrue();
    }

    @Test
    void itIsTheAgendaSource() {
        assertThat(new AgendaDashboardSource(listEvents, listTasksDue).kind()).isEqualTo(CardKind.AGENDA);
    }

    private AgendaCard read() {
        return (AgendaCard) new AgendaDashboardSource(listEvents, listTasksDue)
            .read(new DashboardContext(caller, "me@test.com", TODAY, SpaceType.SHARED)).card();
    }

    private static CalendarOccurrence allDay(String title, LocalDate start, LocalDate end) {
        return new CalendarOccurrence(CalendarSourceType.EVENT, UUID.randomUUID().toString(), null, null, true,
            title, null, null, true, start, null, end, null, null, List.of());
    }

    private static CalendarOccurrence timed(String title, LocalDate startDate, LocalTime start, LocalDate endDate, LocalTime end) {
        return new CalendarOccurrence(CalendarSourceType.EVENT, UUID.randomUUID().toString(), null, null, true,
            title, null, null, false, startDate, start, endDate, end, null, List.of());
    }

    private Task task(String title, TaskPriority priority) {
        return new Task(UUID.randomUUID(), spaceId, title, TaskStatus.TODO, priority, TODAY, List.of(me), List.of(),
            null, me, Instant.parse("2026-01-01T00:00:00Z").plusSeconds(created++));
    }
}
