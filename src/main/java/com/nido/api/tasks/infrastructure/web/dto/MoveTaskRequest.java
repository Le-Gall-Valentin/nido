package com.nido.api.tasks.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveTaskRequest(@NotNull UUID destinationSpaceId) {}
