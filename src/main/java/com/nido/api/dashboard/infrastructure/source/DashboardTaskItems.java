package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.Task;

/** How a task reaches the dashboard, and whose it is — shared by the agenda and the tasks sources. */
final class DashboardTaskItems {

    private DashboardTaskItems() {}

    static TaskItem from(Task task) {
        int done = (int) task.subtasks().stream().filter(Subtask::done).count();
        return new TaskItem(task.id(), task.title(), task.dueDate(), task.priority().name(), task.status().name(),
            task.assigneeIds(), done, task.subtasks().size(), task.recurringSeriesId() != null);
    }

    /**
     * Assigned to the caller or to nobody. In a shared space, somebody else's task does not call for
     * the caller's action; in a personal space every task is the owner's.
     */
    static boolean isMine(Task task, DashboardContext context) {
        return !context.isShared()
            || task.assigneeIds().isEmpty()
            || task.assigneeIds().contains(context.callerId());
    }
}
