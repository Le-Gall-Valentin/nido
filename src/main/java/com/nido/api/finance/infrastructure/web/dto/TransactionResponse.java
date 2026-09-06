package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
    UUID id, String label, BigDecimal amount, TransactionType type, UUID categoryId, LocalDate date,
    UUID payerId, List<ContributionResponse> contributors, boolean recurring
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.id(), t.label(), t.amount(), t.type(), t.categoryId(), t.date(), t.payerId(),
            t.contributors().stream().map(ContributionResponse::from).toList(), t.recurringSeriesId() != null);
    }
}
