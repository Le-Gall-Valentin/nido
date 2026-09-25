package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.finance.application.port.in.ListSavingsGoalsUseCase;
import com.nido.api.finance.application.port.in.ListTransactionsInRangeUseCase;
import com.nido.api.finance.application.port.in.ProjectRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.kitchen.application.port.in.ListMenuEntriesUseCase;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.application.port.in.ListTasksDueBetweenUseCase;
import com.nido.api.tasks.application.port.in.ProjectRecurringTasksUseCase;
import com.nido.api.tasks.domain.model.ProjectedTaskOccurrence;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The four adapters that read another bounded context. Each is a mapping, and each is tested as one. */
class ExternalCalendarSourcesTest {

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    private final LocalDate from = LocalDate.of(2026, 1, 1);
    private final LocalDate to = LocalDate.of(2026, 1, 31);

    @Test
    void taskSourceReadsOnlyTheTasksDueInTheWindow() {
        ListTasksDueBetweenUseCase tasks = mock(ListTasksDueBetweenUseCase.class);
        ProjectRecurringTasksUseCase projection = mock(ProjectRecurringTasksUseCase.class);
        when(tasks.list(caller, from, to)).thenReturn(List.of(task("Dans la fenêtre", LocalDate.of(2026, 1, 15))));
        when(projection.project(caller, from, to)).thenReturn(List.of());

        assertThat(new TaskCalendarSource(tasks, projection).occurrencesBetween(caller, from, to))
            .extracting(CalendarOccurrence::title).containsExactly("Dans la fenêtre");
    }

    @Test
    void taskSourceMarksTasksAllDayAndCarriesTheirAssignees() {
        ListTasksDueBetweenUseCase tasks = mock(ListTasksDueBetweenUseCase.class);
        ProjectRecurringTasksUseCase projection = mock(ProjectRecurringTasksUseCase.class);
        UUID assignee = UUID.randomUUID();
        when(tasks.list(caller, from, to)).thenReturn(List.of(new Task(
            UUID.randomUUID(), spaceId, "Poubelles", TaskStatus.TODO, TaskPriority.MED,
            LocalDate.of(2026, 1, 15), List.of(assignee), List.of(), null, UUID.randomUUID(), Instant.now())));
        when(projection.project(caller, from, to)).thenReturn(List.of());

        assertThat(new TaskCalendarSource(tasks, projection).occurrencesBetween(caller, from, to))
            .singleElement()
            .satisfies(o -> {
                assertThat(o.allDay()).isTrue();
                assertThat(o.startTime()).isNull();
                assertThat(o.participantIds()).containsExactly(assignee);
                assertThat(o.source()).isEqualTo(CalendarSourceType.TASK);
            });
    }

    @Test
    void taskSourceMarksProjectedOccurrencesAsNotMaterialized() {
        ListTasksDueBetweenUseCase tasks = mock(ListTasksDueBetweenUseCase.class);
        ProjectRecurringTasksUseCase projection = mock(ProjectRecurringTasksUseCase.class);
        when(tasks.list(caller, from, to)).thenReturn(List.of());
        when(projection.project(caller, from, to)).thenReturn(List.of(new ProjectedTaskOccurrence(
            UUID.randomUUID(), "Poubelles", TaskPriority.MED, LocalDate.of(2026, 1, 22))));

        assertThat(new TaskCalendarSource(tasks, projection).occurrencesBetween(caller, from, to))
            .singleElement().extracting(CalendarOccurrence::materialized).isEqualTo(false);
    }

    @Test
    void financeSourceKeepsOnlyTransactionsTiedToASeries() {
        ListTransactionsInRangeUseCase transactions = mock(ListTransactionsInRangeUseCase.class);
        ProjectRecurringSeriesUseCase projection = mock(ProjectRecurringSeriesUseCase.class);
        when(transactions.list(caller, from, to)).thenReturn(List.of(
            transaction("Courses", null),
            transaction("Loyer", UUID.randomUUID())));
        when(projection.project(caller, from, to)).thenReturn(List.of());

        assertThat(new FinanceCalendarSource(transactions, projection).occurrencesBetween(caller, from, to))
            .extracting(CalendarOccurrence::title).containsExactly("Loyer");
    }

    @Test
    void financeSourceProjectsFutureOccurrencesAsNotMaterialized() {
        ListTransactionsInRangeUseCase transactions = mock(ListTransactionsInRangeUseCase.class);
        ProjectRecurringSeriesUseCase projection = mock(ProjectRecurringSeriesUseCase.class);
        when(transactions.list(caller, from, to)).thenReturn(List.of());
        when(projection.project(caller, from, to)).thenReturn(List.of(new ProjectedOccurrence(
            UUID.randomUUID(), "Loyer", new BigDecimal("850.00"), TransactionType.EXPENSE, LocalDate.of(2026, 1, 28))));

        assertThat(new FinanceCalendarSource(transactions, projection).occurrencesBetween(caller, from, to))
            .singleElement()
            .satisfies(o -> {
                assertThat(o.materialized()).isFalse();
                assertThat(o.allDay()).isTrue();
                assertThat(o.source()).isEqualTo(CalendarSourceType.FINANCE);
            });
    }

    @Test
    void mealSourceNamesAnOccurrenceAfterItsRecipe() {
        ListMenuEntriesUseCase menu = mock(ListMenuEntriesUseCase.class);
        when(menu.list(caller, from, to)).thenReturn(List.of(new MenuEntryView(
            new MenuEntry(UUID.randomUUID(), spaceId, LocalDate.of(2026, 1, 8), UUID.randomUUID(), 4, 0),
            recipe("Gratin dauphinois"))));

        assertThat(new MealCalendarSource(menu).occurrencesBetween(caller, from, to))
            .singleElement()
            .satisfies(o -> {
                assertThat(o.title()).isEqualTo("Gratin dauphinois");
                assertThat(o.allDay()).isTrue();
                assertThat(o.source()).isEqualTo(CalendarSourceType.MEAL);
            });
    }

    @Test
    void savingsSourceKeepsOnlyGoalsWithATargetDateInsideTheWindow() {
        ListSavingsGoalsUseCase goals = mock(ListSavingsGoalsUseCase.class);
        when(goals.list(caller)).thenReturn(List.of(
            goal("Sans échéance", null),
            goal("Hors fenêtre", LocalDate.of(2027, 5, 1)),
            goal("Vacances", LocalDate.of(2026, 1, 20))));

        assertThat(new SavingsCalendarSource(goals).occurrencesBetween(caller, from, to))
            .extracting(CalendarOccurrence::title).containsExactly("Vacances");
    }

    private Task task(String title, LocalDate dueDate) {
        return new Task(UUID.randomUUID(), spaceId, title, TaskStatus.TODO, TaskPriority.MED,
            dueDate, List.of(), List.of(), null, UUID.randomUUID(), Instant.now());
    }

    private Transaction transaction(String label, UUID seriesId) {
        return new Transaction(UUID.randomUUID(), spaceId, label, new BigDecimal("10.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 10), null,
            List.<Contribution>of(), seriesId, Instant.now());
    }

    private SavingsGoalDetail goal(String name, LocalDate targetDate) {
        return new SavingsGoalDetail(new SavingsGoal(UUID.randomUUID(), spaceId, name,
            new BigDecimal("1000.00"), targetDate, "#5c7a58", "🎯"), List.of());
    }

    private Recipe recipe(String name) {
        return new Recipe(
            UUID.randomUUID(), spaceId, name, null, RecipeCategory.PLAT,
            30, 4, false, List.of(), List.of(), null, Instant.now(), Instant.now());
    }
}
