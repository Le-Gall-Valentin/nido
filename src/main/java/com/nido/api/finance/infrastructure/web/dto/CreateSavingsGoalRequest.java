package com.nido.api.finance.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSavingsGoalRequest(
    @NotBlank @Size(max = 100) String name, @NotNull @DecimalMin("0.01") BigDecimal targetAmount, LocalDate targetDate
) {}
