package com.nido.api.dashboard.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A task as the dashboard shows it. Its priority and status are the dashboard's own enums, spelled as
 * the tasks module spells them — the client reads those names.
 */
public record TaskItem(UUID id, String title, LocalDate dueDate, Priority priority, Status status,
                       List<UUID> assigneeIds, int subtasksDone, int subtasksTotal, boolean recurring) {

    public TaskItem {
        assigneeIds = List.copyOf(assigneeIds);
    }

    public enum Priority { HIGH, MED, LOW }

    public enum Status { TODO, DOING, DONE }
}
