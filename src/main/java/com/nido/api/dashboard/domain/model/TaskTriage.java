package com.nido.api.dashboard.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * The tasks card and its "À traiter" item, decided from the space's open tasks in board order. Three
 * groups that never overlap and never repeat the agenda: overdue, the next six days, and what is in
 * progress with no date close enough to be in either. Tasks due today live in the agenda only.
 *
 * <p>The order is the one given — the tasks module's board order — and is never decided here.
 */
public final class TaskTriage {

    /** Rows per group sent to the client; the counts still see every task. */
    public static final int GROUP_CAP = 20;
    /** Titles named by the overdue item; its count still sees every overdue task. */
    public static final int TITLES_IN_ATTENTION = 3;

    private TaskTriage() {}

    public static SourceResult of(List<TaskItem> openInBoardOrder, DashboardContext context) {
        LocalDate today = context.today();
        LocalDate weekEnd = today.plusDays(6);

        List<TaskItem> overdue = keep(openInBoardOrder,
            task -> task.dueDate() != null && task.dueDate().isBefore(today));
        List<TaskItem> thisWeek = keep(openInBoardOrder,
            task -> task.dueDate() != null && task.dueDate().isAfter(today) && !task.dueDate().isAfter(weekEnd));
        List<TaskItem> inProgress = keep(openInBoardOrder,
            task -> task.status() == TaskItem.Status.DOING && (task.dueDate() == null || task.dueDate().isAfter(weekEnd)));

        List<TaskItem> overdueMine = keep(overdue, context::isMine);
        List<AttentionItem> attention = overdueMine.isEmpty()
            ? List.of()
            : List.of(new AttentionItem.OverdueTasks(overdueMine.size(),
                overdueMine.stream().limit(TITLES_IN_ATTENTION).map(TaskItem::title).toList()));

        if (overdue.isEmpty() && thisWeek.isEmpty() && inProgress.isEmpty()) {
            return SourceResult.attentionOnly(attention);
        }
        int openCountMine = keep(openInBoardOrder, context::isMine).size();
        return SourceResult.of(new TasksCard(capped(overdue), capped(thisWeek), capped(inProgress),
            openInBoardOrder.size(), openCountMine), attention);
    }

    private static List<TaskItem> keep(List<TaskItem> tasks, Predicate<TaskItem> rule) {
        return tasks.stream().filter(rule).toList();
    }

    private static List<TaskItem> capped(List<TaskItem> tasks) {
        return tasks.stream().limit(GROUP_CAP).toList();
    }
}
