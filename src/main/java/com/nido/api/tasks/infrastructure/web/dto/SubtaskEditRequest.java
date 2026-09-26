package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.SubtaskEdit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** One line of an edited subtask list: {@code id} names one the task has, null makes a new one. */
public record SubtaskEditRequest(UUID id, @NotBlank @Size(max = 200) String text) {
    public SubtaskEdit toEdit() {
        return new SubtaskEdit(id, text);
    }
}
