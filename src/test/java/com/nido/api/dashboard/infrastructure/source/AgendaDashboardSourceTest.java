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
    void splitsTodaysEventsIntoAllDayAndTimedInTimeOrder() {
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(
            timed("Dîner chez Paul", TODAY, LocalTime.of(19, 30), TODAY, LocalTime.of(22, 0)),
            allDay("Anniversaire de Léa", TODAY, TODAY),
            timed("Marché", TODAY, LocalTime.of(10, 30), TODAY, LocalTime.of(12, 0))));

        AgendaCard card = read();

        assertThat(card.allDay()).extracting(AgendaEvent::title).containsExactly("Anniversaire de Léa");
        assertThat(card.timed()).extracting(AgendaEvent::title).containsExactly("Marché", "Dîner chez Paul");
    }

    @Test
    void aMultiDayEventThatStartedYesterdayIsAnAllDayEntryToday() {
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(
            timed("Garde de nuit", TODAY.minusDays(1), LocalTime.of(22, 0), TODAY, LocalTime.of(6, 0))));

        AgendaCard card = read();

        assertThat(card.allDay()).extracting(AgendaEvent::title).containsExactly("Garde de nuit");
        assertThat(card.timed()).isEmpty();
    }

    @Test
    void aTimedEventStartingTodayIsOnTodaysTimelineAtItsStartWheneverItEnds() {
        // An evening that runs past midnight used to become a banner "until tomorrow", its 22:00 lost.
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(
            timed("Soirée chez Paul", TODAY, LocalTime.of(22, 0), TOMORROW, LocalTime.of(1, 0)),
            timed("Dîner", TODAY, LocalTime.of(19, 30), TODAY, LocalTime.of(21, 0)),
            timed("Séminaire", TODAY, LocalTime.of(9, 0), TODAY.plusDays(2), LocalTime.of(17, 0))));

        AgendaCard card = read();

        assertThat(card.timed()).extracting(AgendaEvent::title).containsExactly("Séminaire", "Dîner", "Soirée chez Paul");
        assertThat(card.allDay()).isEmpty();
    }

    @Test
    void anEventOnlyTomorrowIsNotToday() {
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(
            timed("Brunch", TOMORROW, LocalTime.of(11, 0), TOMORROW, LocalTime.of(13, 0))));

        AgendaCard card = read();

        assertThat(card.allDay()).isEmpty();
        assertThat(card.timed()).isEmpty();
    }

    @Test
    void tomorrowIsTheFirstEventStartingTomorrowAllDayFirst() {
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(
            allDay("Vacances", TODAY, TOMORROW),
            timed("Brunch", TOMORROW, LocalTime.of(11, 0), TOMORROW, LocalTime.of(13, 0)),
            allDay("Fête du quartier", TOMORROW, TOMORROW)));

        assertThat(read().tomorrow().title()).isEqualTo("Fête du quartier");
    }

    @Test
    void tomorrowIsTheEarliestTimedEventWhenNoneIsAllDay() {
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of(
            timed("Piano", TOMORROW, LocalTime.of(18, 0), TOMORROW, LocalTime.of(19, 0)),
            timed("Brunch", TOMORROW, LocalTime.of(11, 0), TOMORROW, LocalTime.of(13, 0))));

        assertThat(read().tomorrow().title()).isEqualTo("Brunch");
    }

    @Test
    void anEmptyDayStillGivesAFullCardWithNothingTomorrow() {
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of());

        assertThat(read()).isEqualTo(new AgendaCard(List.of(), List.of(), List.of(), null));
    }

    @Test
    void dueTodayKeepsOpenTasksOnlyWithMineFirst() {
        UUID someoneElse = UUID.randomUUID();
        when(listEvents.list(caller, TODAY, TOMORROW)).thenReturn(List.of());
        when(listTasksDue.list(caller, TODAY, TODAY)).thenReturn(List.of(
            task("someone else's", TaskStatus.TODO, List.of(someoneElse)),
            task("done", TaskStatus.DONE, List.of(me)),
            task("mine", TaskStatus.DOING, List.of(me)),
            task("nobody's", TaskStatus.TODO, List.of())));

        assertThat(read().dueToday()).extracting(TaskItem::title)
            .containsExactly("mine", "nobody's", "someone else's");
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

    private Task task(String title, TaskStatus status, List<UUID> assignees) {
        return new Task(UUID.randomUUID(), spaceId, title, status, TaskPriority.MED, TODAY, assignees, List.of(),
            null, me, Instant.parse("2026-01-01T00:00:00Z").plusSeconds(created++));
    }
}
