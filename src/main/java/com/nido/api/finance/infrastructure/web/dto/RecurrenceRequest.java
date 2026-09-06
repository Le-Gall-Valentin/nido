package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.RecurrenceInterval;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RecurrenceRequest(
    @NotNull RecurrenceInterval intervalType, @Min(1) int intervalCount, @NotNull LocalDate anchorDate, LocalDate endDate
) {}
