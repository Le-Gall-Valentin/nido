package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Transaction(
    UUID id, UUID spaceId, String label, BigDecimal amount, TransactionType type, UUID categoryId,
    LocalDate date, UUID payerId, List<Contribution> contributors, UUID recurringSeriesId, Instant createdAt
) {
    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(contributors, "contributors");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
