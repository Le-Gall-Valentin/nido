package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTaskStatusRequest(@NotNull TaskStatus status) {}
