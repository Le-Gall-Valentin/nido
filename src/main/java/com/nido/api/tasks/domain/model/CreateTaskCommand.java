package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateTaskCommand(
    UUID spaceId, String title, TaskPriority priority, LocalDate dueDate,
    List<UUID> assigneeIds, List<SubtaskInput> subtasks, UUID recurringSeriesId
) {
    public CreateTaskCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(assigneeIds, "assigneeIds");
        Objects.requireNonNull(subtasks, "subtasks");
    }
}
