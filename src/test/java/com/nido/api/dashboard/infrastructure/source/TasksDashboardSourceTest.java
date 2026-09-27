package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.dashboard.domain.model.TasksCard;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.tasks.application.port.in.ListOpenTasksUseCase;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TasksDashboardSourceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private final UUID spaceId = UUID.randomUUID();
    private final UUID me = UUID.randomUUID();
    private final UUID someoneElse = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, me, SpaceRole.MEMBER, Instant.now());
    private final ListOpenTasksUseCase listOpenTasks = mock(ListOpenTasksUseCase.class);
    private int created;

    @Test
    void theThreeGroupsNeverOverlapAndLeaveTodaysTasksToTheAgenda() {
        when(listOpenTasks.list(caller)).thenReturn(List.of(
            task("overdue", TaskStatus.TODO, TODAY.minusDays(1), List.of()),
            task("today", TaskStatus.TODO, TODAY, List.of()),
            task("doing today", TaskStatus.DOING, TODAY, List.of()),
            task("in three days", TaskStatus.TODO, TODAY.plusDays(3), List.of()),
            task("in six days", TaskStatus.TODO, TODAY.plusDays(6), List.of()),
            task("next week", TaskStatus.TODO, TODAY.plusDays(7), List.of()),
            task("doing later", TaskStatus.DOING, TODAY.plusDays(10), List.of()),
            task("doing undated", TaskStatus.DOING, null, List.of()),
            task("todo undated", TaskStatus.TODO, null, List.of())));

        TasksCard card = (TasksCard) read(SpaceType.SHARED).card();

        assertThat(card.overdue()).extracting(TaskItem::title).containsExactly("overdue");
        assertThat(card.thisWeek()).extracting(TaskItem::title).containsExactly("in three days", "in six days");
        assertThat(card.inProgress()).extracting(TaskItem::title).containsExactly("doing later", "doing undated");
        assertThat(card.openCount()).isEqualTo(9);
    }

    @Test
    void theOverdueAttentionCountsOnlyMyTasksAndThoseOfNobody() {
        when(listOpenTasks.list(caller)).thenReturn(List.of(
            task("mine", TaskStatus.TODO, TODAY.minusDays(3), List.of(me)),
            task("nobody's", TaskStatus.TODO, TODAY.minusDays(2), List.of()),
            task("someone else's", TaskStatus.TODO, TODAY.minusDays(1), List.of(someoneElse))));

        SourceResult result = read(SpaceType.SHARED);

        assertThat(((TasksCard) result.card()).overdue()).hasSize(3);
        assertThat(result.attention()).containsExactly(new AttentionItem.OverdueTasks(2, List.of("mine", "nobody's")));
    }

    @Test
    void inAPersonalSpaceEveryOverdueTaskIsMine() {
        when(listOpenTasks.list(caller)).thenReturn(List.of(
            task("assigned elsewhere", TaskStatus.TODO, TODAY.minusDays(1), List.of(someoneElse))));

        assertThat(read(SpaceType.PERSONAL).attention())
            .containsExactly(new AttentionItem.OverdueTasks(1, List.of("assigned elsewhere")));
    }

    @Test
    void theAttentionNamesAtMostThreeTasksButCountsThemAll() {
        List<Task> overdue = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            overdue.add(task("late " + i, TaskStatus.TODO, TODAY.minusDays(10 - i), List.of(me)));
        }
        when(listOpenTasks.list(caller)).thenReturn(overdue);

        assertThat(read(SpaceType.SHARED).attention())
            .containsExactly(new AttentionItem.OverdueTasks(5, List.of("late 1", "late 2", "late 3")));
    }

    @Test
    void noTaskInAnyGroupMeansNoCardAndNoAttention() {
        when(listOpenTasks.list(caller)).thenReturn(List.of(
            task("next week", TaskStatus.TODO, TODAY.plusDays(7), List.of()),
            task("undated", TaskStatus.TODO, null, List.of()),
            task("today", TaskStatus.TODO, TODAY, List.of())));

        SourceResult result = read(SpaceType.SHARED);

        assertThat(result.card()).isNull();
        assertThat(result.attention()).isEmpty();
    }

    @Test
    void eachGroupIsCappedAtTwentyWhileTheCountsSeeEveryTask() {
        List<Task> overdue = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            overdue.add(task("late " + i, TaskStatus.TODO, TODAY.minusDays(1), List.of(me)));
        }
        when(listOpenTasks.list(caller)).thenReturn(overdue);

        SourceResult result = read(SpaceType.SHARED);

        assertThat(((TasksCard) result.card()).overdue()).hasSize(20);
        assertThat(((TasksCard) result.card()).openCount()).isEqualTo(25);
        assertThat(result.attention()).containsExactly(
            new AttentionItem.OverdueTasks(25, List.of("late 0", "late 1", "late 2")));
    }

    @Test
    void openCountMineCountsMineAndUnassigned() {
        when(listOpenTasks.list(caller)).thenReturn(List.of(
            task("mine", TaskStatus.TODO, TODAY.plusDays(1), List.of(me)),
            task("nobody's", TaskStatus.TODO, null, List.of()),
            task("someone else's", TaskStatus.TODO, TODAY.plusDays(2), List.of(someoneElse))));

        assertThat(((TasksCard) read(SpaceType.SHARED).card()).openCountMine()).isEqualTo(2);
    }

    @Test
    void anItemCarriesItsSubtaskProgressPriorityStatusAndRecurrence() {
        Task recurring = new Task(UUID.randomUUID(), spaceId, "Poubelles", TaskStatus.TODO, TaskPriority.HIGH,
            TODAY.plusDays(2), List.of(me),
            List.of(new Subtask(UUID.randomUUID(), "sortir", true),
                new Subtask(UUID.randomUUID(), "rentrer", false),
                new Subtask(UUID.randomUUID(), "laver", false)),
            UUID.randomUUID(), me, Instant.parse("2026-01-01T00:00:00Z"));
        when(listOpenTasks.list(caller)).thenReturn(List.of(recurring));

        TaskItem item = ((TasksCard) read(SpaceType.SHARED).card()).thisWeek().getFirst();

        assertThat(item).isEqualTo(new TaskItem(recurring.id(), "Poubelles", TODAY.plusDays(2), TaskItem.Priority.HIGH, TaskItem.Status.TODO,
            List.of(me), 1, 3, true));
    }

    @Test
    void itIsTheTasksSource() {
        assertThat(new TasksDashboardSource(listOpenTasks).kind()).isEqualTo(CardKind.TASKS);
    }

    private SourceResult read(SpaceType type) {
        return new TasksDashboardSource(listOpenTasks).read(new DashboardContext(caller, "me@test.com", TODAY, type));
    }

    /** Same priority for every task, and a strictly increasing creation time, so the order is by due date, then input. */
    private Task task(String title, TaskStatus status, LocalDate dueDate, List<UUID> assignees) {
        return new Task(UUID.randomUUID(), spaceId, title, status, TaskPriority.MED, dueDate, assignees, List.of(),
            null, me, Instant.parse("2026-01-01T00:00:00Z").plusSeconds(created++));
    }
}
