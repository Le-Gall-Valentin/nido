package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateRecurringSeriesRequest(
    @NotBlank @Size(max = 200) String label, @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount, @NotNull TransactionType type,
    @NotNull UUID categoryId, UUID payerId, List<@Valid ContributionRequest> contributors, @Valid @NotNull RecurrenceRequest recurrence
) {
    public List<ContributionRequest> contributors() {
        return contributors == null ? List.of() : contributors;
    }
}
