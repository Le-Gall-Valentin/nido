package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.TaskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTaskRequest(
    @NotBlank @Size(max = 200) String title, @NotNull TaskPriority priority, LocalDate dueDate,
    List<UUID> assigneeIds, List<@NotBlank String> subtasks, @Valid RecurrenceRequest recurrence
) {
    public List<UUID> assigneeIds() {
        return assigneeIds == null ? List.of() : assigneeIds;
    }

    public List<String> subtasks() {
        return subtasks == null ? List.of() : subtasks;
    }
}
