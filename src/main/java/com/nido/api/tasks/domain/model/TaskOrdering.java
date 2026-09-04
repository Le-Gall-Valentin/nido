package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Computes the backend-controlled kanban card order: priority, then due date, then age. */
public final class TaskOrdering {

    private static final Comparator<Task> ORDER = Comparator
        .comparing(Task::priority)
        .thenComparing(t -> t.dueDate() == null ? LocalDate.MAX : t.dueDate())
        .thenComparing(Task::createdAt);

    private TaskOrdering() {}

    public static List<Task> sort(List<Task> tasks) {
        return tasks.stream().sorted(ORDER).toList();
    }
}
