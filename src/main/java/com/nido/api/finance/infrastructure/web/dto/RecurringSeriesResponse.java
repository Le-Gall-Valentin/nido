package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RecurringSeriesResponse(
    UUID id, String label, BigDecimal amount, TransactionType type, UUID categoryId, UUID payerId,
    List<ContributionResponse> contributors, RecurrenceInterval intervalType, int intervalCount,
    LocalDate anchorDate, LocalDate endDate
) {
    public static RecurringSeriesResponse from(RecurringTransactionSeries s) {
        return new RecurringSeriesResponse(s.id(), s.label(), s.amount(), s.type(), s.categoryId(), s.payerId(),
            s.contributors().stream().map(ContributionResponse::from).toList(), s.intervalType(), s.intervalCount(),
            s.anchorDate(), s.endDate());
    }
}
