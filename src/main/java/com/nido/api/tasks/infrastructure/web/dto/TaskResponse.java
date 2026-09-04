package com.nido.api.tasks.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
    UUID id, String title, TaskStatus status, TaskPriority priority,
    // No global "omit nulls" Jackson config exists in this codebase (every
    // other nullable DTO field, e.g. ShoppingItemResponse.quantity, is
    // serialized as an explicit `null`) — this field alone must be omitted
    // entirely once DONE, so it opts in to NON_NULL individually rather
    // than changing that codebase-wide default.
    @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate dueDate,
    List<UUID> assigneeIds, List<SubtaskResponse> subtasks, boolean recurring
) {
    public static TaskResponse from(Task t) {
        // A completed task never reports a due date — see Global Constraints.
        LocalDate visibleDueDate = t.status() == TaskStatus.DONE ? null : t.dueDate();
        return new TaskResponse(t.id(), t.title(), t.status(), t.priority(), visibleDueDate,
            t.assigneeIds(), t.subtasks().stream().map(SubtaskResponse::from).toList(), t.recurringSeriesId() != null);
    }
}
