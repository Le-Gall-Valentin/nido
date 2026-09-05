package com.nido.api.finance.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SettleDebtRequest(@NotNull UUID fromMemberId, @NotNull UUID toMemberId, @NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull LocalDate date) {}
