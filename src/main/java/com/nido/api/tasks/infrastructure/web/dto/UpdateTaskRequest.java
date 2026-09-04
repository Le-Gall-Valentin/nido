package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateTaskRequest(
    @NotBlank @Size(max = 200) String title, @NotNull TaskPriority priority, LocalDate dueDate, List<UUID> assigneeIds
) {
    public List<UUID> assigneeIds() {
        return assigneeIds == null ? List.of() : assigneeIds;
    }
}
