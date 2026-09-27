package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgendaDayTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        SpaceRole.MEMBER, Instant.now());

    @Test
    void splitsTodaysEventsIntoAllDayAndTimedInTimeOrder() {
        AgendaCard card = day(
            timed("Dîner chez Paul", TODAY, LocalTime.of(19, 30), TODAY, LocalTime.of(22, 0)),
            allDay("Anniversaire de Léa", TODAY, TODAY),
            timed("Marché", TODAY, LocalTime.of(10, 30), TODAY, LocalTime.of(12, 0)));

        assertThat(card.allDay()).extracting(AgendaEvent::title).containsExactly("Anniversaire de Léa");
        assertThat(card.timed()).extracting(AgendaEvent::title).containsExactly("Marché", "Dîner chez Paul");
    }

    @Test
    void aMultiDayEventThatStartedYesterdayIsAnAllDayEntryToday() {
        AgendaCard card = day(timed("Garde de nuit", TODAY.minusDays(1), LocalTime.of(22, 0), TODAY, LocalTime.of(6, 0)));

        assertThat(card.allDay()).extracting(AgendaEvent::title).containsExactly("Garde de nuit");
        assertThat(card.timed()).isEmpty();
    }

    @Test
    void aTimedEventStartingTodayIsOnTodaysTimelineAtItsStartWheneverItEnds() {
        // An evening that runs past midnight used to become a banner "until tomorrow", its 22:00 lost.
        AgendaCard card = day(
            timed("Soirée chez Paul", TODAY, LocalTime.of(22, 0), TOMORROW, LocalTime.of(1, 0)),
            timed("Dîner", TODAY, LocalTime.of(19, 30), TODAY, LocalTime.of(21, 0)),
            timed("Séminaire", TODAY, LocalTime.of(9, 0), TODAY.plusDays(2), LocalTime.of(17, 0)));

        assertThat(card.timed()).extracting(AgendaEvent::title).containsExactly("Séminaire", "Dîner", "Soirée chez Paul");
        assertThat(card.allDay()).isEmpty();
    }

    @Test
    void anEventOnlyTomorrowIsNotToday() {
        AgendaCard card = day(timed("Brunch", TOMORROW, LocalTime.of(11, 0), TOMORROW, LocalTime.of(13, 0)));

        assertThat(card.allDay()).isEmpty();
        assertThat(card.timed()).isEmpty();
    }

    @Test
    void tomorrowIsTheFirstEventStartingTomorrowAllDayFirst() {
        AgendaCard card = day(
            allDay("Vacances", TODAY, TOMORROW),
            timed("Brunch", TOMORROW, LocalTime.of(11, 0), TOMORROW, LocalTime.of(13, 0)),
            allDay("Fête du quartier", TOMORROW, TOMORROW));

        assertThat(card.tomorrow().title()).isEqualTo("Fête du quartier");
    }

    @Test
    void tomorrowIsTheEarliestTimedEventWhenNoneIsAllDay() {
        AgendaCard card = day(
            timed("Piano", TOMORROW, LocalTime.of(18, 0), TOMORROW, LocalTime.of(19, 0)),
            timed("Brunch", TOMORROW, LocalTime.of(11, 0), TOMORROW, LocalTime.of(13, 0)));

        assertThat(card.tomorrow().title()).isEqualTo("Brunch");
    }

    @Test
    void anEmptyDayStillGivesAFullCardWithNothingTomorrow() {
        assertThat(day()).isEqualTo(new AgendaCard(List.of(), List.of(), List.of(), null));
    }

    @Test
    void dueTodayKeepsOpenTasksOnlyMineFirstAndOtherwiseInTheOrderGiven() {
        List<TaskItem> dueTodayInBoardOrder = List.of(
            task("someone else's", TaskItem.Status.TODO, false),
            task("done", TaskItem.Status.DONE, true),
            task("mine", TaskItem.Status.DOING, true),
            task("nobody's", TaskItem.Status.TODO, true));

        assertThat(AgendaDay.of(List.of(), dueTodayInBoardOrder, context()).dueToday()).extracting(TaskItem::title)
            .containsExactly("mine", "nobody's", "someone else's");
    }

    private AgendaCard day(AgendaEvent... events) {
        return AgendaDay.of(List.of(events), List.of(), context());
    }

    private DashboardContext context() {
        return new DashboardContext(caller, "me@test.com", TODAY, SpaceType.SHARED);
    }

    private static AgendaEvent allDay(String title, LocalDate start, LocalDate end) {
        return new AgendaEvent(UUID.randomUUID().toString(), title, null, null, start, end, null, null, List.of());
    }

    private static AgendaEvent timed(String title, LocalDate startDate, LocalTime start, LocalDate endDate, LocalTime end) {
        return new AgendaEvent(UUID.randomUUID().toString(), title, null, null, startDate, endDate, start, end, List.of());
    }

    private static TaskItem task(String title, TaskItem.Status status, boolean mine) {
        return new TaskItem(UUID.randomUUID(), title, TODAY, TaskItem.Priority.MED, status, List.of(), 0, 0, false, mine);
    }
}
