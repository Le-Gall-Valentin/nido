package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.TaskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateRecurringTaskSeriesRequest(
    @NotBlank @Size(max = 200) String title, @NotNull TaskPriority priority,
    List<@NotBlank String> subtasks, @Valid @NotNull RecurrenceRequest recurrence
) {
    public List<String> subtasks() {
        return subtasks == null ? List.of() : subtasks;
    }
}
