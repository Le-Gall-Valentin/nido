package com.nido.api.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferEventRequest(@NotNull UUID destinationSpaceId) {}
