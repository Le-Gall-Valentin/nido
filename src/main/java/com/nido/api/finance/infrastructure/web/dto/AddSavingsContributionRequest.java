package com.nido.api.finance.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AddSavingsContributionRequest(@NotNull UUID memberId, @NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull LocalDate date) {}
