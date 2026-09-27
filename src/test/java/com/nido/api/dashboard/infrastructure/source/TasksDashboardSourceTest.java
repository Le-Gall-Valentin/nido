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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TasksDashboardSourceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private final UUID spaceId = UUID.randomUUID();
    private final UUID me = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, me, SpaceRole.MEMBER, Instant.now());
    private final ListOpenTasksUseCase listOpenTasks = mock(ListOpenTasksUseCase.class);
    private int created;

    @Test
    void readsTheCallersOpenTasksAndLetsTheTriageDecide() {
        // The rules are TaskTriage's (see TaskTriageTest); this only checks they get the space's open tasks.
        when(listOpenTasks.list(caller)).thenReturn(List.of(
            task("Filtre de la hotte", TaskStatus.TODO, TODAY.minusDays(2), List.of(me))));

        SourceResult result = read(SpaceType.SHARED);

        assertThat(((TasksCard) result.card()).overdue()).extracting(TaskItem::title).containsExactly("Filtre de la hotte");
        assertThat(result.attention()).containsExactly(new AttentionItem.OverdueTasks(1, List.of("Filtre de la hotte")));
    }

    @Test
    void keepsTheBoardOrderTheTasksModuleAlreadyGives() {
        // ListOpenTasksUseCase answers in board order, and that order is the tasks module's to decide:
        // whatever it says comes first stays first, even against the due dates.
        when(listOpenTasks.list(caller)).thenReturn(List.of(
            task("due yesterday", TaskStatus.TODO, TODAY.minusDays(1), List.of()),
            task("due last week", TaskStatus.TODO, TODAY.minusDays(7), List.of())));

        assertThat(((TasksCard) read(SpaceType.SHARED).card()).overdue()).extracting(TaskItem::title)
            .containsExactly("due yesterday", "due last week");
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
            List.of(me), 1, 3, true, true));
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
