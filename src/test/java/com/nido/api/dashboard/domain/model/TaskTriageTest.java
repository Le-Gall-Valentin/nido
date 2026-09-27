package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTriageTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private final UUID me = UUID.randomUUID();
    private final UUID someoneElse = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), me, SpaceRole.MEMBER, Instant.now());

    @Test
    void theThreeGroupsNeverOverlapAndLeaveTodaysTasksToTheAgenda() {
        TasksCard card = (TasksCard) triage(SpaceType.SHARED,
            task("overdue", TaskItem.Status.TODO, TODAY.minusDays(1), List.of()),
            task("today", TaskItem.Status.TODO, TODAY, List.of()),
            task("doing today", TaskItem.Status.DOING, TODAY, List.of()),
            task("in three days", TaskItem.Status.TODO, TODAY.plusDays(3), List.of()),
            task("in six days", TaskItem.Status.TODO, TODAY.plusDays(6), List.of()),
            task("next week", TaskItem.Status.TODO, TODAY.plusDays(7), List.of()),
            task("doing later", TaskItem.Status.DOING, TODAY.plusDays(10), List.of()),
            task("doing undated", TaskItem.Status.DOING, null, List.of()),
            task("todo undated", TaskItem.Status.TODO, null, List.of())).card();

        assertThat(card.overdue()).extracting(TaskItem::title).containsExactly("overdue");
        assertThat(card.thisWeek()).extracting(TaskItem::title).containsExactly("in three days", "in six days");
        assertThat(card.inProgress()).extracting(TaskItem::title).containsExactly("doing later", "doing undated");
        assertThat(card.openCount()).isEqualTo(9);
    }

    @Test
    void keepsTheOrderItIsGiven() {
        // The board order is the tasks module's to decide; the triage never sorts again.
        TasksCard card = (TasksCard) triage(SpaceType.SHARED,
            task("due yesterday", TaskItem.Status.TODO, TODAY.minusDays(1), List.of()),
            task("due last week", TaskItem.Status.TODO, TODAY.minusDays(7), List.of())).card();

        assertThat(card.overdue()).extracting(TaskItem::title).containsExactly("due yesterday", "due last week");
    }

    @Test
    void theOverdueAttentionCountsOnlyMyTasksAndThoseOfNobody() {
        SourceResult result = triage(SpaceType.SHARED,
            task("mine", TaskItem.Status.TODO, TODAY.minusDays(3), List.of(me)),
            task("nobody's", TaskItem.Status.TODO, TODAY.minusDays(2), List.of()),
            task("someone else's", TaskItem.Status.TODO, TODAY.minusDays(1), List.of(someoneElse)));

        assertThat(((TasksCard) result.card()).overdue()).hasSize(3);
        assertThat(result.attention()).containsExactly(new AttentionItem.OverdueTasks(2, List.of("mine", "nobody's")));
    }

    @Test
    void inAPersonalSpaceEveryOverdueTaskIsMine() {
        assertThat(triage(SpaceType.PERSONAL,
            task("assigned elsewhere", TaskItem.Status.TODO, TODAY.minusDays(1), List.of(someoneElse))).attention())
            .containsExactly(new AttentionItem.OverdueTasks(1, List.of("assigned elsewhere")));
    }

    @Test
    void theAttentionNamesAtMostThreeTasksButCountsThemAll() {
        List<TaskItem> overdue = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            overdue.add(task("late " + i, TaskItem.Status.TODO, TODAY.minusDays(10 - i), List.of(me)));
        }

        assertThat(TaskTriage.of(overdue, context(SpaceType.SHARED)).attention())
            .containsExactly(new AttentionItem.OverdueTasks(5, List.of("late 1", "late 2", "late 3")));
    }

    @Test
    void noTaskInAnyGroupMeansNoCardAndNoAttention() {
        SourceResult result = triage(SpaceType.SHARED,
            task("next week", TaskItem.Status.TODO, TODAY.plusDays(7), List.of()),
            task("undated", TaskItem.Status.TODO, null, List.of()),
            task("today", TaskItem.Status.TODO, TODAY, List.of()));

        assertThat(result.card()).isNull();
        assertThat(result.attention()).isEmpty();
    }

    @Test
    void eachGroupIsCappedWhileTheCountsSeeEveryTask() {
        List<TaskItem> overdue = new ArrayList<>();
        for (int i = 0; i < TaskTriage.GROUP_CAP + 5; i++) {
            overdue.add(task("late " + i, TaskItem.Status.TODO, TODAY.minusDays(1), List.of(me)));
        }

        SourceResult result = TaskTriage.of(overdue, context(SpaceType.SHARED));

        assertThat(((TasksCard) result.card()).overdue()).hasSize(20);
        assertThat(((TasksCard) result.card()).openCount()).isEqualTo(25);
        assertThat(result.attention()).containsExactly(
            new AttentionItem.OverdueTasks(25, List.of("late 0", "late 1", "late 2")));
    }

    @Test
    void openCountMineCountsMineAndUnassigned() {
        TasksCard card = (TasksCard) triage(SpaceType.SHARED,
            task("mine", TaskItem.Status.TODO, TODAY.plusDays(1), List.of(me)),
            task("nobody's", TaskItem.Status.TODO, null, List.of()),
            task("someone else's", TaskItem.Status.TODO, TODAY.plusDays(2), List.of(someoneElse))).card();

        assertThat(card.openCountMine()).isEqualTo(2);
    }

    private SourceResult triage(SpaceType type, TaskItem... openInBoardOrder) {
        return TaskTriage.of(List.of(openInBoardOrder), context(type));
    }

    private DashboardContext context(SpaceType type) {
        return new DashboardContext(caller, "me@test.com", TODAY, type);
    }

    private static TaskItem task(String title, TaskItem.Status status, LocalDate dueDate, List<UUID> assignees) {
        return new TaskItem(UUID.randomUUID(), title, dueDate, TaskItem.Priority.MED, status, assignees, 0, 0, false);
    }
}
