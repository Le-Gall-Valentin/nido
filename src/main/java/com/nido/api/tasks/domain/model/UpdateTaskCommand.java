package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record UpdateTaskCommand(
    UUID taskId, UUID spaceId, String title, TaskPriority priority, LocalDate dueDate, List<UUID> assigneeIds
) {
    public UpdateTaskCommand {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(assigneeIds, "assigneeIds");
    }
}
