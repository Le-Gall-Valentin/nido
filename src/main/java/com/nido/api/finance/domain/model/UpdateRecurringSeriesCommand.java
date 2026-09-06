package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record UpdateRecurringSeriesCommand(
    UUID seriesId, UUID spaceId, String label, BigDecimal amount, TransactionType type, UUID categoryId,
    UUID payerId, List<ContributionInput> contributors, RecurrenceInterval intervalType, int intervalCount,
    LocalDate anchorDate, LocalDate endDate
) {
    public UpdateRecurringSeriesCommand {
        Objects.requireNonNull(seriesId, "seriesId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(contributors, "contributors");
        Objects.requireNonNull(intervalType, "intervalType");
        Objects.requireNonNull(anchorDate, "anchorDate");
    }
}
