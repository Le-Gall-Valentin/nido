package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.TaskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * {@code subtasks} is the whole list in its new order. Left out, it leaves the subtasks alone —
 * unlike {@code assigneeIds}, whose absence clears them: a frontend from before subtask editing
 * never sends it, and must not erase every list it touches.
 */
public record UpdateTaskRequest(
    @NotBlank @Size(max = 200) String title, @NotNull TaskPriority priority, LocalDate dueDate, List<UUID> assigneeIds,
    List<@NotNull @Valid SubtaskEditRequest> subtasks
) {
    public List<UUID> assigneeIds() {
        return assigneeIds == null ? List.of() : assigneeIds;
    }
}
