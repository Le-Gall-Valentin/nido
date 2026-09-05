package com.nido.api.finance.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveTransactionRequest(@NotNull UUID destinationSpaceId) {}
