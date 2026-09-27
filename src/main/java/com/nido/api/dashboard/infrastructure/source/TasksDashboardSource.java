package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.dashboard.domain.model.TasksCard;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.tasks.application.port.in.ListOpenTasksUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Three groups that never overlap and never repeat the agenda: overdue, the next six days, and what
 * is in progress with no date close enough to be in either. Tasks due today live in the agenda only.
 */
@Component
public class TasksDashboardSource implements DashboardSource {

    static final int GROUP_CAP = 20;
    static final int TITLES_IN_ATTENTION = 3;

    private final ListOpenTasksUseCase listOpenTasks;

    public TasksDashboardSource(ListOpenTasksUseCase listOpenTasks) {
        this.listOpenTasks = listOpenTasks;
    }

    @Override
    public CardKind kind() {
        return CardKind.TASKS;
    }

    @Override
    public SourceResult read(DashboardContext context) {
        LocalDate today = context.today();
        LocalDate weekEnd = today.plusDays(6);
        // Already in board order (ListOpenTasksUseCase): the tasks module decides it, the dashboard keeps it.
        List<Task> open = listOpenTasks.list(context.caller());

        List<Task> overdue = open.stream()
            .filter(task -> task.dueDate() != null && task.dueDate().isBefore(today))
            .toList();
        List<Task> thisWeek = open.stream()
            .filter(task -> task.dueDate() != null && task.dueDate().isAfter(today) && !task.dueDate().isAfter(weekEnd))
            .toList();
        List<Task> inProgress = open.stream()
            .filter(task -> task.status() == TaskStatus.DOING
                && (task.dueDate() == null || task.dueDate().isAfter(weekEnd)))
            .toList();

        List<Task> overdueMine = overdue.stream().filter(task -> DashboardTaskItems.isMine(task, context)).toList();
        List<AttentionItem> attention = overdueMine.isEmpty()
            ? List.of()
            : List.of(new AttentionItem.OverdueTasks(overdueMine.size(),
                overdueMine.stream().limit(TITLES_IN_ATTENTION).map(Task::title).toList()));

        if (overdue.isEmpty() && thisWeek.isEmpty() && inProgress.isEmpty()) {
            return SourceResult.attentionOnly(attention);
        }
        int openCountMine = (int) open.stream().filter(task -> DashboardTaskItems.isMine(task, context)).count();
        return SourceResult.of(
            new TasksCard(capped(overdue), capped(thisWeek), capped(inProgress), open.size(), openCountMine),
            attention);
    }

    private static List<TaskItem> capped(List<Task> tasks) {
        return tasks.stream().limit(GROUP_CAP).map(DashboardTaskItems::from).toList();
    }
}
