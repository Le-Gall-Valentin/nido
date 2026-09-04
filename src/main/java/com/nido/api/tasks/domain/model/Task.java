package com.nido.api.tasks.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Task(
    UUID id, UUID spaceId, String title, TaskStatus status, TaskPriority priority,
    LocalDate dueDate, List<UUID> assigneeIds, List<Subtask> subtasks,
    UUID recurringSeriesId, Instant createdAt
) {
    public Task {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(assigneeIds, "assigneeIds");
        Objects.requireNonNull(subtasks, "subtasks");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
