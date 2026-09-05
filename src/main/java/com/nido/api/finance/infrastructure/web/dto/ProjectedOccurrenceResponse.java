package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectedOccurrenceResponse(UUID seriesId, String label, BigDecimal amount, TransactionType type, LocalDate date) {
    public static ProjectedOccurrenceResponse from(ProjectedOccurrence o) {
        return new ProjectedOccurrenceResponse(o.seriesId(), o.label(), o.amount(), o.type(), o.date());
    }
}
