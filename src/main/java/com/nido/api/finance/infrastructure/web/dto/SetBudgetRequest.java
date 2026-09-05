package com.nido.api.finance.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetBudgetRequest(@NotNull @DecimalMin(value = "0.00") BigDecimal monthlyLimit) {}
