package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;

/** How a task of the tasks module reaches the dashboard — shared by the agenda and the tasks sources. */
final class DashboardTaskItems {

    private DashboardTaskItems() {}

    static TaskItem from(Task task) {
        int done = (int) task.subtasks().stream().filter(Subtask::done).count();
        return new TaskItem(task.id(), task.title(), task.dueDate(), priorityOf(task.priority()), statusOf(task.status()),
            task.assigneeIds(), done, task.subtasks().size(), task.recurringSeriesId() != null);
    }

    /** Exhaustive on purpose: a priority the tasks module adds does not compile until the dashboard knows it. */
    static TaskItem.Priority priorityOf(TaskPriority priority) {
        return switch (priority) {
            case HIGH -> TaskItem.Priority.HIGH;
            case MED -> TaskItem.Priority.MED;
            case LOW -> TaskItem.Priority.LOW;
        };
    }

    static TaskItem.Status statusOf(TaskStatus status) {
        return switch (status) {
            case TODO -> TaskItem.Status.TODO;
            case DOING -> TaskItem.Status.DOING;
            case DONE -> TaskItem.Status.DONE;
        };
    }
}
