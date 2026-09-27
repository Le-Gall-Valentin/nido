package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.dashboard.domain.model.TaskTriage;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.tasks.application.port.in.ListOpenTasksUseCase;
import org.springframework.stereotype.Component;

import java.util.List;

/** Reads the space's open tasks and lets {@link TaskTriage} decide the card and the overdue item. */
@Component
public class TasksDashboardSource implements DashboardSource {

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
        // Already in board order (ListOpenTasksUseCase): the tasks module decides it, the dashboard keeps it.
        List<TaskItem> open = listOpenTasks.list(context.caller()).stream()
            .map(task -> DashboardTaskItems.from(task, context))
            .toList();
        return TaskTriage.of(open, context);
    }
}
