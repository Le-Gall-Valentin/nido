package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code subtasks} is the task's whole list in its new order, or null to leave the subtasks as
 * they are — what a client that predates subtask editing sends, and must not erase them by it.
 */
public record UpdateTaskCommand(
    UUID taskId, UUID spaceId, String title, TaskPriority priority, LocalDate dueDate, List<UUID> assigneeIds,
    List<SubtaskEdit> subtasks
) {
    public UpdateTaskCommand {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(assigneeIds, "assigneeIds");
    }
}
